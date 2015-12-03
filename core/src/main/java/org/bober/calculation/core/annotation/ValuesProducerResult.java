package org.bober.calculation.core.annotation;

import org.bober.calculation.core.interfaces.ValuesProducer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * describe which ValuesProducer should be executed
 * and which value of resultName of it should be passed to field
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValuesProducerResult {
    String NO_EXPRESSION = "noExpression";
    String NO_EXP_ALIAS = "noExpressionAlias";

    Class<? extends ValuesProducer> producer();
    String resultName() default ValuesProducer.RESULT;
    boolean required() default true;
    String exp() default NO_EXPRESSION;
    String expAlias() default NO_EXP_ALIAS;
}
