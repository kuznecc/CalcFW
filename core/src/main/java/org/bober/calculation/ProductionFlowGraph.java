package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public class ProductionFlowGraph implements ProductionFlow {

    @Override
    public <T> T produceClass(Class<T> dtoClass){
        return produceClass(dtoClass, null, null);
    }

    @Override
    public <T> T produceClass(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedProductionCtx) {
        ProductionContextBuilderGraph builder = new ProductionContextBuilderGraph(appCtx, preparedProductionCtx);
        T dto = builder.buildClass(dtoClass);
        return dto;
    }

}
