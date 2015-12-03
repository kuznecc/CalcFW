package org.bober.calculation.core;

import org.springframework.context.ApplicationContext;

public class CalculationFlow {

    public <T> T produceDto(Class<T> dtoClass){
        return produceDto(dtoClass, null);
    }

    public <T> T produceDto(Class<T> dtoClass, ApplicationContext appCtx) {
        ProducersContextBuilder builder = new ProducersContextBuilder(appCtx);
        T dto = builder.buildDto(dtoClass);
        return dto;
    }

}
