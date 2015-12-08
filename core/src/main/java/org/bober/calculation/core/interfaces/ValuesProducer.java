package org.bober.calculation.core.interfaces;

import java.util.Map;

/**
 * producer with obtain all dependencies and produce a resultName map.
 */
public interface ValuesProducer {
    String RESULT = "result";

    default void produce() { } /* todo : remove default method implementation */
    default boolean isItCalculated() { return false;} /* todo : remove default method implementation */
    Map<String, Object> getResult();

}
