package org.bober.calculation.impl;

import org.bober.calculation.CalcContextBuilder;
import org.bober.calculation.CalcFlowException;
import org.bober.calculation.ContextBuilderUtil;
import org.bober.calculation.ValuesProducer;
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
public class CalcContextBuilderRecursion implements CalcContextBuilder {

    @Override
    public <T> T buildClass(Class<T> clazz, ApplicationContext springAppCtx, Map<Class, Object> preparedProducersCtx) {
        Map<Class, Object> producersCtx = preparedProducersCtx != null ? preparedProducersCtx : new HashMap<>();

        try {
            instantiateProducersFromClassAnnotations(clazz, springAppCtx, producersCtx);
            instantiateClassesRecursivelyAndWireResults(clazz, springAppCtx, producersCtx);
        } catch (CalcFlowException e) {
            String msg = String.format("Error due building class %s", clazz.getName());
            throw new RuntimeException(msg, e);
        }

        return (T) producersCtx.get(clazz);
    }

    private void instantiateProducersFromClassAnnotations(Class<?> clazz, ApplicationContext springAppCtx,
                                                          Map<Class, Object> ctx)
            throws CalcFlowException {
        List<Class> relatedClasses = ContextBuilderUtil.buildReversedClassInherentChain(clazz);
        for (Class rClazz : relatedClasses) {
            if (rClazz.isAnnotationPresent(PrepareValuesProducer.class)) {
                Class<? extends ValuesProducer>[] onClassProducers =
                        ((Class<?>) rClazz).getAnnotation(PrepareValuesProducer.class).value();
                for (Class<? extends ValuesProducer> producerClass : onClassProducers) {
                    try {
                        instantiateClassesRecursivelyAndWireResults(producerClass, springAppCtx, ctx);
                    } catch (CalcFlowException e) {
                        throw new CalcFlowException("Processing @PrepareValuesProducer", e);
                    }
                }
            }
        }
    }

    private void instantiateClassesRecursivelyAndWireResults(Class clazz, ApplicationContext springAppCtx,
                                                             Map<Class, Object> ctx)
            throws CalcFlowException {
        if (ctx.containsKey(clazz)) {
            return;
        }

        Object instance;
        try {
            instance = ContextBuilderUtil.makeNewInstance(clazz, springAppCtx);
            List<Field> classFieldsWithRespectToParents = ContextBuilderUtil.fetchClassFields(clazz);
            for (Field field : classFieldsWithRespectToParents) {
                if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                    Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                    instantiateClassesRecursivelyAndWireResults(producerClass, springAppCtx, ctx);

                    ContextBuilderUtil.passProducerResultToField(instance, field, ctx);
                }
            }
        } catch (CalcFlowException e) {
            String msg = String.format("instantiation %s", clazz.getName());
            throw new CalcFlowException(msg,e);
        }

        ContextBuilderUtil.putInstanceToCtx(instance, ctx);
    }




}
