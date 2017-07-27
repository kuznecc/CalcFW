package org.bober.calculation.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import org.bober.calculation.CalcContextBuilder;
import org.bober.calculation.CalcFlowException;
import org.bober.calculation.ContextBuilderUtil;
import org.bober.calculation.ValuesProducer;
import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// All processed classes should have only one instance
public class CalcContextBuilderGraph implements CalcContextBuilder {
    private static final Map<Class, SetMultimap<Class, Class>> cachedRelations = new HashMap<>();
    private static final Map<Class, Set<Class>> cachedAllRelatedClasses = new HashMap<>();

    private Set<Class> producerInterfaceImplementations; // from @PrepareValuesProducers
    //todo: eliminate Guava usage
    private SetMultimap<Class, Class> relations = HashMultimap.create(); // key-parentClass, val-childClass
    private Set<Class> preparedClasses;
    private Set<Class> allRelatedClasses;
    protected Map<Class, AtomicInteger> relationsCounters = new HashMap<>();
    protected Map<Class, ChainedWrapper> wrappers = new HashMap<>();
    private Map<Class, Object> instancesCtx;

    @Override
    public <T> T buildClass(Class<T> clazz, ApplicationContext springAppCtx, Map<Class, Object> preparedProducersCtx) {
        instancesCtx = preparedProducersCtx != null ? preparedProducersCtx : new HashMap<>();
        preparedClasses = Collections.unmodifiableSet(instancesCtx.keySet());
        cachedRelations(clazz);
        cachedAllRelatedClasses(clazz);
        initRelationCounters();
        initWrappers(springAppCtx);

        initiateCalculationChain();

        return getInstance(clazz);
    }

    protected  <T> T getInstance(Class<T> clazz) {
        return (T) instancesCtx.get(clazz);
    }

    protected void initiateCalculationChain() {
        // execute all wrappers that have no relations with other classes
        relationsCounters.keySet().stream()
                .filter(c -> relationsCounters.get(c).get() == 0)
                .forEach(c -> wrappers.get(c).instantiate());
    }


    private void initWrappers(ApplicationContext springAppCtx) {
        for (Class clazz : allRelatedClasses) {
            wrappers.put(clazz,
                    makeInstanceWrapper(clazz, wrappers, relations, springAppCtx, instancesCtx, relationsCounters));
        }
    }

    protected ChainedWrapper makeInstanceWrapper(Class clazz,
                                                 Map<Class, ChainedWrapper> wrappers,
                                                 SetMultimap<Class, Class> relations,
                                                 ApplicationContext springApplicationContext,
                                                 Map<Class, Object> instancesCtx,
                                                 Map<Class, AtomicInteger> relationsCounters) {
        return new ChainedWrapper(clazz, wrappers, relations, springApplicationContext, instancesCtx, relationsCounters);
    }

    private void initRelationCounters() {
        for (Class clazz : allRelatedClasses) {
            int counter = !relations.containsKey(clazz) ? 0 :
                    (int) relations.get(clazz).stream()
                            .filter(c -> !preparedClasses.contains(c))
                            .count();
            relationsCounters.put(clazz, new AtomicInteger(counter));
        }
    }

    private void cachedAllRelatedClasses(Class clazz) {
        if (!cachedAllRelatedClasses.containsKey(clazz)) {
            initAllRelatedClasses();
            cachedAllRelatedClasses.put(clazz, allRelatedClasses);
        }
        allRelatedClasses = cachedAllRelatedClasses.get(clazz);
    }

    private void initAllRelatedClasses() {
        allRelatedClasses = new HashSet<>();
        allRelatedClasses.addAll(relations.keySet());
        allRelatedClasses.addAll(relations.values());
    }

    private void cachedRelations(Class clazz) {
        if (!cachedRelations.containsKey(clazz)) {
            processOnClassAnnotation(clazz);
            initRelations(clazz);
            cachedRelations.put(clazz, relations);
        }
        relations = cachedRelations.get(clazz);
    }

    private void processOnClassAnnotation(Class<?> clazz) {
        producerInterfaceImplementations = new HashSet<>();

        List<Class> relatedClasses = ContextBuilderUtil.buildReversedClassInherentChain(clazz);
        for (Class rClazz : relatedClasses) {
            if (rClazz.isAnnotationPresent(PrepareValuesProducer.class)) {
                producerInterfaceImplementations.addAll(Arrays.asList(
                        ((Class<?>) rClazz).getAnnotation(PrepareValuesProducer.class).value()));
            }
        }
    }

    private void initRelations(Class clazz) {
        if (clazz.isInterface()) {
            clazz = matchInterfaceToClass(clazz);
        }

        List<Field> classFields = ContextBuilderUtil.fetchClassFields(clazz);

        for (Field field : classFields) {
            if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();
                if (producerClass.isInterface()) {
                    producerClass = matchInterfaceToClass(producerClass);
                }

                initRelations(producerClass);
                relations.put(clazz, producerClass);
            }
        }
    }

    private Class matchInterfaceToClass(final Class interf) {
        return producerInterfaceImplementations.stream()
                .filter(c -> interf.isAssignableFrom(c))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Can't found implementation class for interface " + interf.getName()));
    }

    static class ChainedWrapper {
        protected Class clazz;
        private Map<Class, ChainedWrapper> wrappers;
        private final ApplicationContext springApplicationContext;
        protected final Map<Class, Object> instancesCtx;
        private final AtomicInteger myRelationsCounter;
        private final Set<Class> myResultConsumers;

        public ChainedWrapper(Class clazz,
                              Map<Class, ChainedWrapper> wrappers,
                              Multimap<Class, Class> relations,
                              ApplicationContext springApplicationContext,
                              Map<Class, Object> instancesCtx,
                              Map<Class, AtomicInteger> relationsCounters) {
            this.clazz = clazz;
            this.wrappers = wrappers;
            this.springApplicationContext = springApplicationContext;
            this.instancesCtx = instancesCtx;
            myRelationsCounter = relationsCounters.get(clazz);
            myResultConsumers = relations.keySet().stream()
                    .filter(c -> relations.get(c).contains(clazz))
                    .collect(Collectors.toSet());
        }

        public void instantiate() {
            if (!instancesCtx.containsKey(clazz)) {
                instantiationTask();
                resolveMyRelations();
            }
        }

        protected void instantiationTask() {
            try {
                Object instance = ContextBuilderUtil.makeNewInstance(clazz, springApplicationContext);
                passProducerResultsToInstance(clazz, instance);
                ContextBuilderUtil.putInstanceToCtx(instance, instancesCtx);
            } catch (CalcFlowException e) {
                throw new RuntimeException("can't make instance for " + clazz.getSimpleName(), e);
            }
        }

        private void passProducerResultsToInstance(Class clazz, Object instance) {
            List<Field> classFieldsWithRespectToParents = ContextBuilderUtil.fetchClassFields(clazz);
            classFieldsWithRespectToParents.stream()
                    .filter(field -> field.isAnnotationPresent(ValuesProducerResult.class))
                    .forEach(field -> passProducerResultsToField(clazz, instance, field));
        }

        private void passProducerResultsToField(Class clazz, Object instance, Field field) {
            try {
                ContextBuilderUtil.passProducerResultToField(instance, field, instancesCtx);
            } catch (CalcFlowException e) {
                throw new RuntimeException("Saturating of " + clazz.getSimpleName() + "|" + e.getMessage(), e);
            }
        }

        protected void resolveMyRelations() {
            for (Class resultConsumer : myResultConsumers) {
                wrappers.get(resultConsumer).resolveRelation();
            }
        }

        private void resolveRelation() {
            int counter = myRelationsCounter.decrementAndGet();
            if (counter == 0) {
                instantiate();
            }
        }
    }
}
