package org.bober.calculation.core;

import org.bober.calculation.core.annotation.PrepareValuesProducer;
import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

/**
 *  Process Class {
 *      1. On class annotation 'PrepareValuesProducer' {
 *          - instantiate all listed producers for future usage and put they to ctx.
 *          }
 *      2. Instantiate specified producer
 *      3. Iterate fields for annotation 'ValuesProducerResult' {
 *          - if ctx doesn't contain producer from annotation :
 *                      execute recursively p2-4 for ValuesProducer that mentioned in annotation.
 *          - Spring @Autowire TODO
 *          - get needed producer from ctx.
 *          - prepare ProducerResults and pass it to field
 *          }
 *      4. put instance to ctx
 *  }
 *  TODO: Replace result wrappers with real values
 *  TODO: Add logic like @Autowired(required=false)
 *  ProducerResult - it's container that will invoke lazy calculation process of all related producers
 */
public class ProducersContextBuilder {

    public static <T> T buildDto(Class<T> dtoClazz) {
        HashMap<Object, Object> ctx = new HashMap<>();

        try {
            instantiateProducersFromDtoClassAnnotation(dtoClazz, ctx);
            instantiateDtoAndProducersRecursivelyAndWireResults(dtoClazz, ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }

        T dto = ctx.containsKey(dtoClazz) ? (T) ctx.get(dtoClazz) : null;

        return dto;
    }

    private static void instantiateProducersFromDtoClassAnnotation(Class<?> clazz, Map ctx) throws Exception {
        if (clazz.isAnnotationPresent(PrepareValuesProducer.class)) {
            Class<? extends ValuesProducer>[] onClassProducers = clazz.getAnnotation(PrepareValuesProducer.class).value();
            for (Class<? extends ValuesProducer> producerClass : onClassProducers) {
                instantiateDtoAndProducersRecursivelyAndWireResults(producerClass, ctx);
            }
        }
    }

    private static void instantiateDtoAndProducersRecursivelyAndWireResults(Class clazz, Map ctx) throws Exception{
        if (ctx.containsKey(clazz)) {
            return;
        }

        Object instance = clazz.newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                instantiateDtoAndProducersRecursivelyAndWireResults(producerClass, ctx);

                passProducerResultToField(instance, field, ctx);
            }
        }

        putInstanceToCtx(instance, ctx);
    }

    /**
     * Put instance to ctx with few id's.
     * Id it's object class and all implemented interfaces that extend ValuesProducer interface.
     */
    private static void putInstanceToCtx(Object instance, Map ctx) {
        Class<?> clazz = instance.getClass();
        ctx.put(clazz, instance);

        Class<?>[] interfaces = clazz.getInterfaces();

        for (Class<?> anInterface : interfaces) {
            if (ValuesProducer.class.isAssignableFrom(anInterface)) {
                ctx.put(anInterface, instance);
            }
        }
    }

    private static void passProducerResultToField(Object instance, Field field, Map ctx) throws Exception{
        Class<ValuesProducer> relatedProducerClass = (Class<ValuesProducer>) field.getAnnotation(ValuesProducerResult.class).producer();
        String relatedProducerResultName = field.getAnnotation(ValuesProducerResult.class).resultName();
        ValuesProducer relatedProducerInstance = (ValuesProducer) ctx.get(relatedProducerClass);

        ProducerResult result = new ProducerResultImpl<>(relatedProducerInstance, relatedProducerResultName);

        field.setAccessible(true);
        field.set(instance, result);
    }

    /* ================================ */
    private static Class getGenericType(Field field) {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        return  (Class<?>) parameterizedType.getActualTypeArguments()[0];
    }

}
