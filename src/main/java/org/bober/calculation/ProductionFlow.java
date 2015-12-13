package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public class ProductionFlow {

    public <T> T produceDto(Class<T> dtoClass){
        return produceDto(dtoClass, null, null);
    }

    public <T> T produceDto(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedProductionCtx) {
        ProductionContextBuilder builder = new ProductionContextBuilder(appCtx);
        T dto = builder.buildClass(dtoClass, preparedProductionCtx);
        return dto;
    }

}
