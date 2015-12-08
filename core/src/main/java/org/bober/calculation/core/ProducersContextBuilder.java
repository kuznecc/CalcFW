package org.bober.calculation.core;

import org.bober.calculation.core.annotation.PrepareValuesProducer;
import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bober.calculation.core.SpELProcessor.evaluateSpelExpression;
import static org.bober.calculation.core.SpELProcessor.isItSpelOnFieldDetected;

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
 *      6. before passing producer result to field we check is it need to process value
 *          with SpEL expression from ValuesProducerResult annotation
 *  }
 *  todo: change api of calculate method so it returns Object/Map instead void. Invocation of setResult(..) will appear under the hood.
 *  todo: ?autowire ValuesProducerResult to field by field type for cases when producer produce few results with different types
 *  todo: ?save producers single results in context without singletonMap wrappers
 *  todo: move here from usage-point AbstractProducer class
 *  todo: make performance test of SpEL expressions execution
 *  todo: need to cache parsed SpEL expressions
 *  todo: need to cache something in the name of performance
 */
public class ProducersContextBuilder {
    private ApplicationContext springApplicationContext;

    public ProducersContextBuilder() {
    }

    public ProducersContextBuilder(ApplicationContext springApplicationContext) {
        this.springApplicationContext = springApplicationContext;
    }

    public <T> T buildDto(Class<T> dtoClazz, Map preparedCalculationCtx) {
        Map calculationCtx = preparedCalculationCtx != null ? preparedCalculationCtx : new HashMap<>();

        instantiateProducersFromClassAnnotations(dtoClazz, calculationCtx);
        instantiateClassesRecursivelyAndWireResults(dtoClazz, calculationCtx);

        T dto = calculationCtx.containsKey(dtoClazz) ? (T) calculationCtx.get(dtoClazz) : null;

        return dto;
    }

    private void instantiateProducersFromClassAnnotations(Class<?> dtoClazz, Map ctx) {
        List<Class<?>> relatedClasses = ContextBuilderUtil.buildReversedClassInherentChain(dtoClazz);
        for (Class<?> clazz : relatedClasses) {
            if (clazz.isAnnotationPresent(PrepareValuesProducer.class)) {
                Class<? extends ValuesProducer>[] onClassProducers = clazz.getAnnotation(PrepareValuesProducer.class).value();
                for (Class<? extends ValuesProducer> producerClass : onClassProducers) {
                    instantiateClassesRecursivelyAndWireResults(producerClass, ctx);
                }
            }
        }
    }

    private void instantiateClassesRecursivelyAndWireResults(Class clazz, Map ctx){
        if (ctx.containsKey(clazz)) {
            return;
        }

        Object instance = ContextBuilderUtil.makeNewInstance(clazz, springApplicationContext);
        List<Field> classFieldsWithRespectToParents = ContextBuilderUtil.fetchClassFields(clazz);
        for (Field field : classFieldsWithRespectToParents) {
            if (field.isAnnotationPresent(ValuesProducerResult.class)) {
                Class<? extends ValuesProducer> producerClass = field.getAnnotation(ValuesProducerResult.class).producer();

                instantiateClassesRecursivelyAndWireResults(producerClass, ctx);

                passProducerResultToField(instance, field, ctx);
            }
        }

        putInstanceToCtx(instance, ctx);
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

        if (producerInstance == null && isResultRequired) {
            throw new IllegalArgumentException("Due processing instance of '" + instance.getClass().getSimpleName() +
                    "' unable to find '" + producerClass.getSimpleName() + "' in context.");
        }
        if (!producerInstance.getResult().containsKey(producerResultName) && isResultRequired) {
            if (isResultRequired) {
                throw new IllegalArgumentException("Due processing instance of '" + instance.getClass().getSimpleName() +
                        "' unable to get result from '" + producerClass.getSimpleName() + "' instance.");
            } else return;
        }

        Object fieldValue = isItSpelOnFieldDetected(field) ?
                evaluateSpelExpression(field, producersCtx) : getProducerResult(producerInstance, producerResultName);

        field.setAccessible(true);
        try {
            field.set(instance, fieldValue);
        } catch (IllegalAccessException e) {
            e.printStackTrace();    // todo: remove this try/catch
        }
    }

    private Object getProducerResult(ValuesProducer producerInstance, String producerResultName) {
        return producerInstance != null && producerInstance.getResult() != null ? producerInstance.getResult().get(producerResultName) : null;
    }

}
