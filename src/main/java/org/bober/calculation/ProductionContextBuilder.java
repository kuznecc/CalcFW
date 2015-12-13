package org.bober.calculation;

import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.bober.calculation.SpELProcessor.evaluateSpelExpression;
import static org.bober.calculation.SpELProcessor.isItSpelOnFieldDetected;

//  todo: do we need to process @PrepareValuesProducer on @ValuesProducerResult
public class ProductionContextBuilder {
    private ApplicationContext springApplicationContext;

    public ProductionContextBuilder() {
    }

    public ProductionContextBuilder(ApplicationContext springApplicationContext) {
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
        List<Class<?>> relatedClasses = ContextBuilderUtil.buildReversedClassInherentChain(clazz);
        for (Class<?> rClazz : relatedClasses) {
            if (rClazz.isAnnotationPresent(PrepareValuesProducer.class)) {
                Class<? extends ValuesProducer>[] onClassProducers = rClazz.getAnnotation(PrepareValuesProducer.class).value();
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

                    passProducerResultToField(instance, field, ctx);
                }
            }
        } catch (ProductionFlowException e) {
            String msg = String.format("instantiation %s", clazz.getName());
            throw new ProductionFlowException(msg,e);
        }

        putInstanceToCtx(instance, ctx);
    }

    /**
     * Put instance to ctx with few id's.
     * Id it's object class and all implemented interfaces that extend ValuesProducer interface.
     */
    private void putInstanceToCtx(Object instance, Map<Class, Object> ctx) {
        Class<?> clazz = instance.getClass();
        ctx.put(clazz, instance);

        Class<?>[] interfaces = clazz.getInterfaces();

        for (Class<?> anInterface : interfaces) {
            if (ValuesProducer.class.isAssignableFrom(anInterface)) {
                ctx.put(anInterface, instance);
            }
        }
    }

    private void passProducerResultToField(Object instance, Field field, Map<Class, Object> producersCtx)
            throws ProductionFlowException {
        ValuesProducerResult    annotation = field.getAnnotation(ValuesProducerResult.class);
        Class<ValuesProducer>   producerClass = (Class<ValuesProducer>) annotation.producer();
        String                  producerResultName = annotation.resultName();
        boolean                 isResultRequired = annotation.required();
        ValuesProducer          producerInstance = (ValuesProducer) producersCtx.get(producerClass);

        try {
            if (producerInstance == null && isResultRequired) {
                String msg = String.format("There are no needed producer in producers context, but it's required. %s",
                        producerClass.getName());
                throw new ProductionFlowException(msg);
            }

            Map<String, Object> producerResult = producerInstance != null ? producerInstance.getResult() : emptyMap();
            if (!producerResult.containsKey(producerResultName) && isResultRequired) {
                String msg = String.format("Producer %s haven't result %s that required",
                        producerClass.getName(), producerResultName);
                throw new ProductionFlowException(msg);
            }

            Object fieldValue;

            if (isItSpelOnFieldDetected(field)) {
                fieldValue = evaluateSpelExpression(field, producersCtx);
            } else {
                fieldValue = getProducerResult(producerInstance, producerResultName);
            }

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            try {
                field.set(instance, fieldValue);
            } catch (IllegalAccessException e) {
                String msg = String.format("Can't pass value %s", fieldValue!=null?fieldValue:null);
                throw new ProductionFlowException(msg, e);
            }

        } catch (ProductionFlowException e) {
            String msg = String.format("Setting value to field %s", field.getName());
            throw new ProductionFlowException(msg);
        }
    }

    private Object getProducerResult(ValuesProducer producer, String resultName) {
        return producer != null && producer.getResult() != null ? producer.getResult().get(resultName) : null;
    }

}
