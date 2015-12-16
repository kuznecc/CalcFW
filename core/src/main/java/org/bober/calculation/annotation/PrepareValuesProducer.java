package org.bober.calculation.annotation;

import org.bober.calculation.ValuesProducer;

import java.lang.annotation.*;

/**
 * describe which ValuesProducer should be executed before other producers that declared on fields
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PrepareValuesProducer {
    Class<? extends ValuesProducer>[] value();
}
