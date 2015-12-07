package org.bober.calculation.core;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public class CalculationFlow {

    public <T> T produceDto(Class<T> dtoClass){
        return produceDto(dtoClass, null, null);
    }

    public <T> T produceDto(Class<T> dtoClass, ApplicationContext appCtx, Map preparedCalculationCtx) {
        ProducersContextBuilder builder = new ProducersContextBuilder(appCtx);
        T dto = builder.buildDto(dtoClass, preparedCalculationCtx);
        return dto;
    }

}
