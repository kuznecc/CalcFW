package org.bober.calculation;

import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public abstract class ProductionContextBuilderMultiThread implements ProductionContextBuilder {

    private static final Map<Class, List<Field>> classAnnotatedFieldsCache = new ConcurrentHashMap<>();
    private static final Map<Class, List<Class>> classRelatedProducersCache = new ConcurrentHashMap<>();

    protected final ApplicationContext springApplicationContext;
    private Map<Class, Object> ctx;

    public ProductionContextBuilderMultiThread(ApplicationContext springApplicationContext) {
        this.springApplicationContext = springApplicationContext;
    }

    @Override
    public <T> T buildClass(Class<T> clazz, Map<Class, Object> preparedProducersCtx) {
        ctx = preparedProducersCtx != null ?
                new ConcurrentHashMap<>(preparedProducersCtx) : new ConcurrentHashMap<>();

        ForkJoinTask task = getInstantiationTask(clazz, null);
        task.fork();
        task.join();

        return getFromCtx(clazz);
    }

    protected  <T> T getFromCtx(Class<T> clazz) {
        return (T) ctx.get(clazz);
    }

    protected abstract <T> ForkJoinTask getInstantiationTask(Class<T> clazz, ForkJoinTask instantiationTask);

    abstract class InstantiationTask extends RecursiveAction {

        protected boolean isItNeedToInstantiate(Class clazz) {
            return !ctx.containsKey(clazz);
        }

        protected void makeAndPutInstanceToContext(Class clazz) {
            try {
                Object instance = ContextBuilderUtil.makeNewInstance(clazz, springApplicationContext);

                for (Field field : getAnnotatedFields(instance.getClass())) {
                    ContextBuilderUtil.passProducerResultToField(instance, field, ctx);
                }

                ContextBuilderUtil.putInstanceToCtx(instance,ctx);
            } catch (ProductionFlowException e) {
                String msg = String.format("new %s |" + e.getMessage(), clazz.getSimpleName());
                throw new RuntimeException(msg, e);
            }
        }

        protected ForkJoinTask[] makeProducersInvocationTasks(Class clazz) {
            List<Class> relatedProducers = getRelatedProducers(clazz);
            ForkJoinTask[] tasks = new ForkJoinTask[relatedProducers.size()];
            for (int i = 0; i < tasks.length; i++) {
                tasks[i] = getInstantiationTask(relatedProducers.get(i), this);
            }
            return tasks;
        }

        protected void instantiateRelatedProducersAndWaitForThey(Class clazz) {
            ForkJoinTask[] producersInvocationTasks = instantiateAndRunRelatedProducers(clazz);

            for (ForkJoinTask producersInvocationTask : producersInvocationTasks) {
                producersInvocationTask.join();
            }
        }

        protected ForkJoinTask[] instantiateAndRunRelatedProducers(Class clazz) {
            ForkJoinTask[] producersInvocationTasks = makeProducersInvocationTasks(clazz);
            ForkJoinTask.invokeAll(producersInvocationTasks);
            return producersInvocationTasks;
        }

        protected List<Class> getRelatedProducers(Class clazz) {
            if (!classRelatedProducersCache.containsKey(clazz)) {
                Stream<Class> onClassProducers = ContextBuilderUtil.buildReversedClassInherentChain(clazz).stream()
                        .filter(c -> c.isAnnotationPresent(PrepareValuesProducer.class))
                        .map(c -> ((Class<?>) c).getAnnotation(PrepareValuesProducer.class).value())
                        .flatMap(Arrays::stream);

                Stream<Class> onFieldProducers = getAnnotatedFields(clazz).stream()
                        .map(field -> (Class) field.getAnnotation(ValuesProducerResult.class).producer())
                        .filter(p-> !p.isInterface());

                List<Class> producers = Stream
                        .concat(onClassProducers, onFieldProducers)
                        .distinct()
                        .collect(toList());

                classRelatedProducersCache.put(clazz, producers);
            }
            return classRelatedProducersCache.get(clazz);
        }

        protected List<Field> getAnnotatedFields(Class clazz) {
            if (!classAnnotatedFieldsCache.containsKey(clazz)) {
                List<Field> fields = ContextBuilderUtil
                        .fetchClassFields(clazz).stream()
                        .filter(field -> field.isAnnotationPresent(ValuesProducerResult.class))
                        .collect(toList());
                classAnnotatedFieldsCache.put(clazz, fields);
            }
            return classAnnotatedFieldsCache.get(clazz);
        }
    }
}
