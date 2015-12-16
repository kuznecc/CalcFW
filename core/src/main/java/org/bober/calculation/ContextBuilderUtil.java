package org.bober.calculation;

import org.bober.calculation.annotation.ValuesProducerResult;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.*;

import static java.util.Collections.emptyMap;
import static org.bober.calculation.SpELProcessor.evaluateSpelExpression;
import static org.bober.calculation.SpELProcessor.isItSpelOnFieldDetected;

public class ContextBuilderUtil {

    private static final Map<Class, List<Field>> classFieldsCache = new HashMap<>();
    private static final Map<Class, List<Class>> reversedClassInherentChainCache = new HashMap<>();


    public static List<Class> buildReversedClassInherentChain(Class clazz) {
        if (!reversedClassInherentChainCache.containsKey(clazz)) {
            reversedClassInherentChainCache.put(clazz, buildReversedClassInherentChain(clazz, new ArrayList<>()));
        }
        return reversedClassInherentChainCache.get(clazz);
    }

    private static List<Class> buildReversedClassInherentChain(Class clazz, List<Class> chain) {
        Class superclass = clazz.getSuperclass();
        if (!superclass.equals(Object.class)) {
            buildReversedClassInherentChain(superclass, chain);
        }
        chain.add(clazz);
        return chain;
    }

    public static List<Field> fetchClassFields(Class clazz) {
        if (!classFieldsCache.containsKey(clazz)) {
            classFieldsCache.put(clazz, fetchClassFields(clazz, new ArrayList<>()));
        }
        return classFieldsCache.get(clazz);
    }

    private static List<Field> fetchClassFields(Class clazz, List<Field> fields) {
        Class superclass = clazz.getSuperclass();
        if (!superclass.equals(Object.class)) {
            fetchClassFields(superclass, fields);
        }
        Field[] myfields = clazz.getDeclaredFields();
        fields.addAll(Arrays.asList(myfields));
        return fields;
    }

    public static Object makeNewInstance(Class clazz, ApplicationContext springApplicationContext)
            throws ProductionFlowException {
        if (springApplicationContext != null) {
            AutowireCapableBeanFactory beanFactory = springApplicationContext.getAutowireCapableBeanFactory();
            Object bean = beanFactory.createBean(clazz);
            beanFactory.autowireBean(bean);
            return bean;
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            String msg = String.format("Can't create an instance of %s via reflection", clazz.getName());
            throw new ProductionFlowException(msg, e);
        }
    }

    public static void passProducerResultToField(Object instance, Field field, Map<Class, Object> producersCtx)
            throws ProductionFlowException {
        ValuesProducerResult annotation = field.getAnnotation(ValuesProducerResult.class);
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
                fieldValue = producerInstance != null && producerInstance.getResult() != null ?
                        producerInstance.getResult().get(producerResultName) : null;
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


}
