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
    private static final Map<Class, List<Class>> contextKeysCache = new HashMap<>();

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
            throws CalcFlowException {
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
            throw new CalcFlowException(msg, e);
        }
    }

    /**
     * Put instance to ctx with few id's.
     * Id it's object class and all implemented interfaces that extend ValuesProducer interface.
     */
    public static void putInstanceToCtx(Object instance, Map<Class, Object> ctx) {
        Class<?> clazz = instance.getClass();
        if (!(contextKeysCache.containsKey(clazz))) {
            addContextKeys(clazz);
        }
        for (Class c : contextKeysCache.get(clazz)) {
            ctx.put(c, instance);
        }
    }

    private static synchronized void addContextKeys(Class clazz) {
        if (!(contextKeysCache.containsKey(clazz))) {
            List<Class> keys = new ArrayList<>();
            keys.add(clazz);

            Arrays.stream(clazz.getInterfaces())
                    .filter(i->!ValuesProducer.class.equals(i))
                    .filter(ValuesProducer.class::isAssignableFrom)
                    .forEach(keys::add);

            contextKeysCache.put(clazz, keys);
        }
    }
    public static void passProducerResultToField(Object instance, Field field, Map<Class, Object> producersCtx)
            throws CalcFlowException {
        ValuesProducerResult annotation = field.getAnnotation(ValuesProducerResult.class);
        Class<ValuesProducer>   producerClass = (Class<ValuesProducer>) annotation.producer();
        String                  producerResultName = annotation.resultName();
        boolean                 isResultRequired = annotation.required();
        ValuesProducer          producerInstance = (ValuesProducer) producersCtx.get(producerClass);

        try {
            if (producerInstance == null && isResultRequired) {
                String msg = String.format("No instance of %s in context.",
                        producerClass.getName());
                throw new CalcFlowException(msg);
            }

            Map<String, Object> producerResult = producerInstance != null ? producerInstance.getResult() : emptyMap();
            if (!producerResult.containsKey(producerResultName) && isResultRequired) {
                String msg = String.format("Producer %s haven't required result %s.",
                        producerClass.getName(), producerResultName);
                throw new CalcFlowException(msg);
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
                throw new CalcFlowException(msg, e);
            }

        } catch (CalcFlowException e) {
            String msg = String.format("set %s", field.getName());
            throw new CalcFlowException(msg, e);
        }
    }


}
