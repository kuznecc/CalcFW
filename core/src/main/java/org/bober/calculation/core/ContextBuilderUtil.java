package org.bober.calculation.core;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContextBuilderUtil {

    public static List<Class<?>> buildReversedClassInherentChain(Class<?> clazz) {
        return buildReversedClassInherentChain(clazz, new ArrayList<>());
    }

    private static List<Class<?>> buildReversedClassInherentChain(Class<?> clazz, List<Class<?>> chain) {
        Class<?> superclass = clazz.getSuperclass();
        if (!superclass.equals(Object.class)) {
            buildReversedClassInherentChain(superclass, chain);
        }
        chain.add(clazz);
        return chain;
    }

    public static List<Field> fetchClassFields(Class clazz) {
        return fetchClassFields(clazz, new ArrayList<>());
    }

    private static List<Field> fetchClassFields(Class clazz, List<Field> fields) {
        Class<?> superclass = clazz.getSuperclass();
        if (!superclass.equals(Object.class)) {
            fetchClassFields(superclass, fields);
        }
        Field[] myfields = clazz.getDeclaredFields();
        fields.addAll(Arrays.asList(myfields));
        return fields;
    }

    public static Object makeNewInstance(Class clazz, ApplicationContext springApplicationContext) {
        if (springApplicationContext != null) {
            AutowireCapableBeanFactory beanFactory = springApplicationContext.getAutowireCapableBeanFactory();
            Object bean = beanFactory.createBean(clazz);
            beanFactory.autowireBean(bean);
            return bean;
        }

        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            String msg = String.format("Can't create na instance of %s.", clazz.getName());
            throw new RuntimeException(msg, e);
        }
    }

}
