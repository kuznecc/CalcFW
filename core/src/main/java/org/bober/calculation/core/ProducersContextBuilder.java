package org.bober.calculation.core;

import org.bober.calculation.core.annotation.PrepareValuesProducer;
import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 *  Process Class {
 *      1. On class annotation 'PrepareValuesProducer' {
 *          - instantiate all listed producers for future usage and put they to ctx.
 *          }
 *      2. Instantiate specified producer
 *      3. @Autowire beans from Spring context via spring annotations
 *      4. Iterate fields for annotation 'ValuesProducerResult' {
 *          - if ctx doesn't contain producer from annotation :
 *                      execute recursively p2-5 for ValuesProducer that mentioned in annotation.
 *          - get needed producer from ctx.
 *          - prepare ProducerResults and pass it to field
 *          }
 *      5. put instance to ctx
 *  }
 *  todo: cover dto class inheritance
 *  todo: covet producer class inheritance
 *  todo: separate spring depended logic for independent usage
 *  todo: make field annotation that can process result of related producer via SpEL
 */
public class ProducersContextBuilder {
    private ApplicationContext springApplicationContext;

    public ProducersContextBuilder() {
    }

    public ProducersContextBuilder(ApplicationContext springApplicationContext) {
        this.springApplicationContext = springApplicationContext;
    }

    public <T> T buildDto(Class<T> dtoClazz) {
        HashMap<Object, Object> calculationCtx = new HashMap<>();

        instantiateProducersFromDtoClassAnnotation(dtoClazz, calculationCtx);
        instantiateDtoAndProducersRecursivelyAndWireResults(dtoClazz, calculationCtx);

        T dto = calculationCtx.containsKey(dtoClazz) ? (T) calculationCtx.get(dtoClazz) : null;

        return dto;
    }

    private void instantiateProducersFromDtoClassAnnotation(Class<?> clazz, Map ctx) {
        if (clazz.isAnnotationPresent(PrepareValuesProducer.class)) {
            Class<? extends ValuesProducer>[] onClassProducers = clazz.getAnnotation(PrepareValuesProducer.class).value();
            for (Class<? extends ValuesProducer> producerClass : onClassProducers) {
                instantiateDtoAndProducersRecursivelyAndWireResults(producerClass, ctx);
            }
        }
    }

    private void instantiateDtoAndProducersRecursivelyAndWireResults(Class clazz, Map ctx){
        if (ctx.containsKey(clazz)) {
            return;
        }

        Object instance = newInstance(clazz);

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                instantiateDtoAndProducersRecursivelyAndWireResults(producerClass, ctx);

                passProducerResultToField(instance, field, ctx);
            }
        }

        putInstanceToCtx(instance, ctx);
    }

    private Object newInstance(Class clazz) {
        if (springApplicationContext != null) {
            AutowireCapableBeanFactory beanFactory = springApplicationContext.getAutowireCapableBeanFactory();
            Object bean = beanFactory.createBean(clazz);
            beanFactory.autowireBean(bean);
            return bean;
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace(); // todo: remove this try/catch
            return null;
        }
    }

    /**
     * Put instance to ctx with few id's.
     * Id it's object class and all implemented interfaces that extend ValuesProducer interface.
     */
    private void putInstanceToCtx(Object instance, Map ctx) {
        Class<?> clazz = instance.getClass();
        ctx.put(clazz, instance);

        Class<?>[] interfaces = clazz.getInterfaces();

        for (Class<?> anInterface : interfaces) {
            if (ValuesProducer.class.isAssignableFrom(anInterface)) {
                ctx.put(anInterface, instance);
            }
        }
    }

    private void passProducerResultToField(Object instance, Field field, Map producersCtx){
        ValuesProducerResult    annotation = field.getAnnotation(ValuesProducerResult.class);
        Class<ValuesProducer>   producerClass = (Class<ValuesProducer>) annotation.producer();
        String                  producerResultName = annotation.resultName();
        boolean                 isResultRequired = annotation.required();
        ValuesProducer          producerInstance = (ValuesProducer) producersCtx.get(producerClass);
        Object                  producerResult = getProducerResult(producerInstance, producerResultName);

        if (producerInstance == null && isResultRequired) {
            throw new IllegalArgumentException("Due processing instance of '" + instance.getClass().getSimpleName() +
                    "' unable to find '" + producerClass.getSimpleName() + "' in context.");
        }
        if (producerResult == null) {
            if (isResultRequired) {
                throw new IllegalArgumentException("Due processing instance of '" + instance.getClass().getSimpleName() +
                        "' unable to get result from '" + producerClass.getSimpleName() + "' instance.");
            } else return;
        }

        field.setAccessible(true);
        try {
            field.set(instance, producerResult);
        } catch (IllegalAccessException e) {
            e.printStackTrace();    // todo: remove this try/catch
        }
    }

    private Object getProducerResult(ValuesProducer producerInstance, String producerResultName) {
        return producerInstance != null && producerInstance.getResult() != null ? producerInstance.getResult().get(producerResultName) : null;
    }

}
