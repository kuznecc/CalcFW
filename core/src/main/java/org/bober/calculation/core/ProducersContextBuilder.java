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
 *
 *  iterate all class fields {
 *      each with @ValuesProducerResult {
 *          - execute this algorithm recursively
 *      }
 *      if all passed or have no any {
 *          - instantiate producer
 *          - set 'producer results' to each field that it need
 *          - put to context
 *      }
 *  }
 *
 *  How to get producer results and how to use this values in other producers?
 *      - instead result value we going to pass wrapper with lazy evaluation.
 *          When you try to get one value - you will activate whole chain of related result calculation
 *          ~ annotations on field: @ValuesProducerResult(producer, result), @Autowired
 *          ~ type of field: ResultWrapper<Type>
 *          ~ Producer : we put instance of it to ctx
 *          ~ Value : +1. we generate wrapper and set it to field manually
 *                     2. we put instance of qualifier to ctx and INVENT MAGIC qualifier logic for spring autowiring.
 *          ~ When we need to wire producer with result of other producer:
 *              - seek in ctx for right wrapper and use. todo
 *              - seek in ctx for right producer instance and use.
 *              - if we can't found it in ctx and it isn't interface than make wrapper, put it to ctx and use.
 *              - recursively invoke this logic for process producer of this result and repeat previous step
 *      -
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

    /** each child of ValuesProducer interface can be used for implementing different producers
                      that can be wired to @ValuesProducerResult via that 'child interface'
    after instantiating of class and setting all fields we gonna put it to ctx with key 'class'
       for case when we need to work with different implementation of producers we will wire they via interfaces.
       so now we will put to ctx this producer instance with their interface as a key.*/
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
