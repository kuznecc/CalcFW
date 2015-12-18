package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public class ProductionFlowRecursion implements ProductionFlow {

    @Override
    public <T> T produceClass(Class<T> dtoClass){
        return produceClass(dtoClass, null, null);
    }

    @Override
    public <T> T produceClass(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedProductionCtx) {
        ProductionContextBuilder builder = new ProductionContextBuilderRecursion(appCtx);
        T dto = builder.buildClass(dtoClass, preparedProductionCtx);
        return dto;
    }

}
