package org.bober.calculation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.bober.calculation.annotation.ValuesProducerResult;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// todo: invent multi-tread approach for calculating producers
// All processed classes should have only one instance
public class GraphProductionContextBuilder {
    //todo: eliminate Guava usave
    private Multimap<Class, Class> relations = ArrayListMultimap.create(); // key-parentClass, val-childClass
    // todo: make it all concurrency safe
    private Set<Class> allClasses = new HashSet<>();
    private Map<Class, AtomicInteger> relationsCounters = new HashMap<>();
    private Map<Class, ChainedWrapper> wrappers = new HashMap<>();
    private Map<Class, Object> instancesCtx = new HashMap<>();

    public <T> T buildClass(Class<T> clazz) {
        initClassSetAndRelations(clazz);
        initRelationCounters();
        initWrappers();
        initiateCalculationChain();

        return (T) instancesCtx.get(clazz);
    }

    private void initiateCalculationChain() {
        // execute all wrappers that have no relations with other
        for (Class clazz : relationsCounters.keySet()) {
            if (relationsCounters.get(clazz).get() == 0) {
                wrappers.get(clazz).instantiate();
            }
        }
    }


    private void initWrappers() {
        for (Class clazz : allClasses) {
            wrappers.put(clazz, new ChainedWrapper(clazz, wrappers, relations, instancesCtx, relationsCounters));
        }
    }

    private void initRelationCounters() {
        for (Class clazz : allClasses) {
            if (!relations.containsKey(clazz)) {
                relationsCounters.put(clazz, new AtomicInteger(0));
            } else {
                if (relationsCounters.containsKey(clazz)) {
                    relationsCounters.get(clazz).incrementAndGet();
                } else {
                    relationsCounters.put(clazz, new AtomicInteger(1));
                }
            }
        }
    }

    private void initClassSetAndRelations(Class clazz) {
        allClasses.add(clazz);
        List<Field> classFieldsWithRespectToParents = ContextBuilderUtil.fetchClassFields(clazz);

        for (Field field : classFieldsWithRespectToParents) {
            if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                initClassSetAndRelations(producerClass);

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
                        throw new RuntimeException("Saturating of " + clazz.getSimpleName(), e);
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
                instantiate();
            }
        }
    }
}
