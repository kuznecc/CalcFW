package org.bober.calculation;

import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// todo: do we need to process @PrepareValuesProducer on @ValuesProducerResult
// todo: add caching of dto and producers structure to eliminate redundant on fields iteration
// todo: ? add loggers ?
public class ProductionContextBuilderRecursion implements ProductionContextBuilder{
    private ApplicationContext springApplicationContext;

    public ProductionContextBuilderRecursion() {

    }

    public ProductionContextBuilderRecursion(ApplicationContext springApplicationContext) {
        this.springApplicationContext = springApplicationContext;
    }

    public <T> T buildClass(Class<T> clazz, Map<Class, Object> preparedProducersCtx) {
        Map<Class, Object> producersCtx = preparedProducersCtx != null ? preparedProducersCtx : new HashMap<>();

        try {
            instantiateProducersFromClassAnnotations(clazz, producersCtx);
            instantiateClassesRecursivelyAndWireResults(clazz, producersCtx);
        } catch (ProductionFlowException e) {
            String msg = String.format("Error due building class %s", clazz.getName());
            throw new RuntimeException(msg, e);
        }

        return (T) producersCtx.get(clazz);
    }

    private void instantiateProducersFromClassAnnotations(Class<?> clazz, Map<Class, Object> ctx)
            throws ProductionFlowException {
        List<Class> relatedClasses = ContextBuilderUtil.buildReversedClassInherentChain(clazz);
        for (Class rClazz : relatedClasses) {
            if (rClazz.isAnnotationPresent(PrepareValuesProducer.class)) {
                Class<? extends ValuesProducer>[] onClassProducers =
                        ((Class<?>) rClazz).getAnnotation(PrepareValuesProducer.class).value();
                for (Class<? extends ValuesProducer> producerClass : onClassProducers) {
                    try {
                        instantiateClassesRecursivelyAndWireResults(producerClass, ctx);
                    } catch (ProductionFlowException e) {
                        throw new ProductionFlowException("Processing @PrepareValuesProducer", e);
                    }
                }
            }
        }
    }

    private void instantiateClassesRecursivelyAndWireResults(Class clazz, Map<Class, Object> ctx)
            throws ProductionFlowException {
        if (ctx.containsKey(clazz)) {
            return;
        }

        Object instance;
        try {
            instance = ContextBuilderUtil.makeNewInstance(clazz, springApplicationContext);
            List<Field> classFieldsWithRespectToParents = ContextBuilderUtil.fetchClassFields(clazz);
            for (Field field : classFieldsWithRespectToParents) {
                if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                    Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                    instantiateClassesRecursivelyAndWireResults(producerClass, ctx);

                    ContextBuilderUtil.passProducerResultToField(instance, field, ctx);
                }
            }
        } catch (ProductionFlowException e) {
            String msg = String.format("instantiation %s", clazz.getName());
            throw new ProductionFlowException(msg,e);
        }

        ContextBuilderUtil.putInstanceToCtx(instance, ctx);
    }




}
