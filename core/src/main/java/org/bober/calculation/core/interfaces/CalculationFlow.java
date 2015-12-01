package org.bober.calculation.core.interfaces;

public interface CalculationFlow {
    <T> T produceDto(Class<T> dtoClass);
}
