package org.bober.calculation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.bober.calculation.annotation.ValuesProducerResult;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// todo: invent multi-tread approach for calculating producers
// All processed classes should have only one instance
public class GraphProductionContextBuilder {
    private static ExecutorService executorService;
    private static final Map<Class, SetMultimap<Class, Class>> cachedRelations = new HashMap<>();
    private static final Map<Class, Set<Class>> cachedAllRelatedClasses = new HashMap<>();

    private static boolean useMultiTread = false;

    //todo: eliminate Guava usave
    private SetMultimap<Class, Class> relations = HashMultimap.create(); // key-parentClass, val-childClass
    // todo: make it all concurrency safe
    private Set<Class> allRelatedClasses;
    private Map<Class, AtomicInteger> relationsCounters = new HashMap<>();
    private Map<Class, ChainedWrapper> wrappers = new HashMap<>();
    private Map<Class, Object> instancesCtx = new HashMap<>();

    public <T> T buildClass(Class<T> clazz) {
        cachedRelations(clazz);
        cachedAllRelatedClasses(clazz);
        initRelationCounters();
        initWrappers();

        if (useMultiTread) {
            executorService = Executors.newFixedThreadPool(10);
        }
        initiateCalculationChain();

        if (useMultiTread) {
            executorService.shutdown();
        }

        return (T) instancesCtx.get(clazz);
    }

    public static void setUseMultiThread(boolean doWeUseIt) {
        useMultiTread = doWeUseIt;
    }

    private void initiateCalculationChain() {
        // execute all wrappers that have no relations with other classes
        relationsCounters.keySet().stream()
                .filter(c -> relationsCounters.get(c).get() == 0)
                .forEach(c -> {
                    wrappers.get(c).instantiate(useMultiTread);
                });
    }


    private void initWrappers() {
        for (Class clazz : allRelatedClasses) {
            wrappers.put(clazz, new ChainedWrapper(clazz, wrappers, relations, instancesCtx, relationsCounters));
        }
    }

    private void initRelationCounters() {
        for (Class clazz : allRelatedClasses) {
            int counter = relations.containsKey(clazz) ? relations.get(clazz).size() : 0;
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
            initRelations(clazz);
            cachedRelations.put(clazz, relations);
        }
        relations = cachedRelations.get(clazz);
    }

    private void initRelations(Class clazz) {
        List<Field> classFields = ContextBuilderUtil.fetchClassFields(clazz);

        for (Field field : classFields) {
            if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                initRelations(producerClass);

                relations.put(clazz, producerClass);
            }
        }
    }

    static class ChainedWrapper {
        private Class clazz;
        private Map<Class, ChainedWrapper> wrappers;
        private final Map<Class, Object> instancesCtx;
        private final AtomicInteger myRelationsCounter;
        private final Set<Class> myResultConsumers;

        public ChainedWrapper(Class clazz,
                              Map<Class, ChainedWrapper> wrappers,
                              Multimap<Class, Class> relations,
                              Map<Class, Object> instancesCtx,
                              Map<Class, AtomicInteger> relationsCounters) {
            this.clazz = clazz;
            this.wrappers = wrappers;
            this.instancesCtx = instancesCtx;
            myRelationsCounter = relationsCounters.get(clazz);
            myResultConsumers = relations.keySet().stream()
                    .filter(c -> relations.get(c).contains(clazz))
                    .collect(Collectors.toSet());
        }

        public void instantiate(boolean useMultiTreads) {
            if (useMultiTreads) {
                CompletableFuture.runAsync(this::instantiate, executorService);
            } else {
                instantiate();
            }

        }

        public void instantiate() {
            if (!instancesCtx.containsKey(clazz)) {
                try {
                    Object instance = ContextBuilderUtil.makeNewInstance(clazz, null);
                    passProducerResultsToInstance(clazz, instance);
                    instancesCtx.put(clazz, instance);
                } catch (ProductionFlowException e) {
                    throw new RuntimeException("can't make instance for " + clazz.getSimpleName(), e);
                }

            }

            resolveMyRelations();
        }

        private void passProducerResultsToInstance(Class clazz, Object instance) {
            List<Field> classFieldsWithRespectToParents = ContextBuilderUtil.fetchClassFields(clazz);
            for (Field field : classFieldsWithRespectToParents) {
                if (field.isAnnotationPresent(ValuesProducerResult.class)) {

                    try {
                        ContextBuilderUtil.passProducerResultToField(instance, field, instancesCtx);
                    } catch (ProductionFlowException e) {
                        throw new RuntimeException("Saturating of " + clazz.getSimpleName() + "|" + e.getMessage(), e);
                    }

                }
            }
        }

        private void resolveMyRelations() {
            for (Class resultConsumer : myResultConsumers) {
                wrappers.get(resultConsumer).resolveRelation();
            }
        }

        public void resolveRelation() {
            int counter = myRelationsCounter.decrementAndGet();
            if (counter == 0) {
                instantiate(useMultiTread);
            }
        }
    }
}
