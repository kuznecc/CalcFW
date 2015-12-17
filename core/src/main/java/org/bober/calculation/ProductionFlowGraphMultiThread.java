package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public class ProductionFlowGraphMultiThread implements ProductionFlow {

    @Override
    public <T> T produceClass(Class<T> dtoClass){
        return produceClass(dtoClass, null, null);
    }

    @Override
    public <T> T produceClass(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedProductionCtx) {
        GraphProductionContextBuilder.setUseMultiThread(true);
        GraphProductionContextBuilder builder = new GraphProductionContextBuilder(appCtx);
        T dto = builder.buildClass(dtoClass);
        return dto;
    }

}
