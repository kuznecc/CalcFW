package org.bober.calculation.core.interfaces;

import org.springframework.context.ApplicationContext;

public interface CalculationFlow {
    <T> T produceDto(Class<T> dtoClass, ApplicationContext appCtx);

    default <T> T produceDto(Class<T> dtoClass){
        return produceDto(dtoClass, null);
    }
}
