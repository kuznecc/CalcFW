package org.bober.calculation;

import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ProductionContextBuilderRecursionMultiThread implements ProductionContextBuilder {
    private ApplicationContext springApplicationContext;

    public ProductionContextBuilderRecursionMultiThread() {

    }

    public ProductionContextBuilderRecursionMultiThread(ApplicationContext springApplicationContext) {
        this.springApplicationContext = springApplicationContext;
    }

    @Override
    public <T> T buildClass(Class<T> clazz, Map<Class, Object> preparedProducersCtx) {
        Map<Class, Object> producersCtx = preparedProducersCtx != null ?
                new ConcurrentHashMap<>(preparedProducersCtx) : new ConcurrentHashMap<>();

        InstantiationTask task = new InstantiationTask(clazz, producersCtx, springApplicationContext);
        task.fork();
        task.join();

        return (T) producersCtx.get(clazz);
    }


    static class InstantiationTask extends RecursiveAction {
        private static final Map<Class, List<Field>> annotatedFields = new ConcurrentHashMap<>();
        private static final Map<Class, List<Class>> relatedProducers = new ConcurrentHashMap<>();

        private final Class clazz;
        private final Map<Class, Object> ctx;
        private final ApplicationContext springApplicationContext;

        public InstantiationTask(Class clazz, Map<Class, Object> ctx, ApplicationContext springApplicationContext) {
            this.clazz = clazz;
            this.ctx = ctx;
            this.springApplicationContext = springApplicationContext;
        }

        @Override
        protected void compute() {
            try {
                if (ctx.containsKey(clazz)) {
                    return;
                }

                Object instance = ContextBuilderUtil.makeNewInstance(clazz, springApplicationContext);
                System.out.println("new " + clazz.getSimpleName());

                List<InstantiationTask> producersInvocationTasks = getRelatedProducers(clazz).stream()
                        .map(c -> new InstantiationTask(c, ctx, springApplicationContext))
                        .collect(toList());

                ForkJoinTask.invokeAll(producersInvocationTasks);

                producersInvocationTasks.forEach(InstantiationTask::join);

                for (Field field : getAnnotatedFields(clazz)) {
                    ContextBuilderUtil.passProducerResultToField(instance, field, ctx);
                }
                ContextBuilderUtil.putInstanceToCtx(instance,ctx);

            } catch (ProductionFlowException e) {
                String msg = String.format(" %s |" + e.getMessage(), clazz.getSimpleName());
                throw new RuntimeException(msg, e);
            }
        }

        private List<Class> getRelatedProducers(Class clazz) {
            if (!relatedProducers.containsKey(clazz)) {
                Stream<Class> onClassProducers = ContextBuilderUtil.buildReversedClassInherentChain(clazz).stream()
                        .filter(c -> c.isAnnotationPresent(PrepareValuesProducer.class))
                        .map(c -> ((Class<?>) c).getAnnotation(PrepareValuesProducer.class).value())
                        .flatMap(Arrays::stream);

                Stream<Class> onFieldProducers = getAnnotatedFields(clazz).stream()
                        .map(field -> (Class) field.getAnnotation(ValuesProducerResult.class).producer())
                        .filter(p -> !ctx.containsKey(p))
                        .filter(p-> !p.isInterface())
                        .distinct();

                List<Class> producers = Stream
                        .concat(onClassProducers, onFieldProducers)
                        .collect(toList());

                relatedProducers.put(clazz, producers);
            }
            return relatedProducers.get(clazz);
        }

        private List<Field> getAnnotatedFields(Class clazz) {
            if (!annotatedFields.containsKey(clazz)) {
                List<Field> fields = ContextBuilderUtil
                        .fetchClassFields(clazz).stream()
                        .filter(field -> field.isAnnotationPresent(ValuesProducerResult.class))
                        .collect(toList());
                annotatedFields.put(clazz, fields);
            }
            return annotatedFields.get(clazz);
        }
    }
}
