package org.bober.calculation.core;

import org.bober.calculation.core.annotation.PrepareValuesProducer;
import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.bober.calculation.core.SpELProcessor.evaluateSpelExpression;
import static org.bober.calculation.core.SpELProcessor.isItSpelOnFieldDetected;

//  todo: make clear exceptions for whole producing process
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

        instantiateProducersFromClassAnnotations(clazz, producersCtx);
        instantiateClassesRecursivelyAndWireResults(clazz, producersCtx);

        T instance = producersCtx.containsKey(clazz) ? (T) producersCtx.get(clazz) : null;

        return instance;
    }

    private void instantiateProducersFromClassAnnotations(Class<?> clazz, Map<Class, Object> ctx) {
        List<Class<?>> relatedClasses = ContextBuilderUtil.buildReversedClassInherentChain(clazz);
        for (Class<?> rClazz : relatedClasses) {
            if (rClazz.isAnnotationPresent(PrepareValuesProducer.class)) {
                Class<? extends ValuesProducer>[] onClassProducers = rClazz.getAnnotation(PrepareValuesProducer.class).value();
                for (Class<? extends ValuesProducer> producerClass : onClassProducers) {
                    instantiateClassesRecursivelyAndWireResults(producerClass, ctx);
                }
            }
        }
    }

    private void instantiateClassesRecursivelyAndWireResults(Class clazz, Map<Class, Object> ctx){
        if (ctx.containsKey(clazz)) {
            return;
        }

        Object instance = ContextBuilderUtil.makeNewInstance(clazz, springApplicationContext);
        List<Field> classFieldsWithRespectToParents = ContextBuilderUtil.fetchClassFields(clazz);
        for (Field field : classFieldsWithRespectToParents) {
            if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                instantiateClassesRecursivelyAndWireResults(producerClass, ctx);

                passProducerResultToField(instance, field, ctx);
            }
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

    private void passProducerResultToField(Object instance, Field field, Map<Class, Object> producersCtx){
        ValuesProducerResult    annotation = field.getAnnotation(ValuesProducerResult.class);
        Class<ValuesProducer>   producerClass = (Class<ValuesProducer>) annotation.producer();
        String                  producerResultName = annotation.resultName();
        boolean                 isResultRequired = annotation.required();
        ValuesProducer          producerInstance = (ValuesProducer) producersCtx.get(producerClass);

        if (producerInstance == null && isResultRequired) {
            String msg = String.format("Due processing instance of %s unable to find in context producer %s.",
                    instance.getClass().getName(), producerClass.getSimpleName());
            throw new RuntimeException(msg);
        }

        Map<String, Object> producerResult = producerInstance != null ? producerInstance.getResult() : emptyMap();
        if (!producerResult.containsKey(producerResultName) && isResultRequired) {
            String msg = String.format("Due processing instance of %s unable to get from producer %s result %s.",
                    instance.getClass().getName(), producerClass.getSimpleName(), producerResultName);
            throw new RuntimeException(msg);
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
            String msg = String.format("Can't pass value to field %s.%s. (value:%s)",
                    instance.getClass().getName(), field.getName(), fieldValue);
            throw new RuntimeException(msg, e);
        }
    }

    private Object getProducerResult(ValuesProducer producer, String resultName) {
        return producer != null && producer.getResult() != null ? producer.getResult().get(resultName) : null;
    }

}
