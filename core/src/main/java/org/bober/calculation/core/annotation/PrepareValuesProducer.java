package org.bober.calculation.core.annotation;

import org.bober.calculation.core.interfaces.ValuesProducer;

import java.lang.annotation.*;

/**
 * describe which ValuesProducer should be executed before other producers that declared on fields
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PrepareValuesProducer {
    Class<? extends ValuesProducer>[] value();
}
