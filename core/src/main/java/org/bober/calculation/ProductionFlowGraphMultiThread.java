package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public class ProductionFlowGraphMultiThread implements ProductionFlow {

    public void setUseMultiThreading(boolean useMultiThreading) {
        GraphProductionContextBuilder.setUseMultiThread(useMultiThreading);
    }

    @Override
    public <T> T produceClass(Class<T> dtoClass){
        return produceClass(dtoClass, null, null);
    }

    @Override
    public <T> T produceClass(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedProductionCtx) {
        GraphProductionContextBuilder builder = new GraphProductionContextBuilder();
        T dto = builder.buildClass(dtoClass);
        return dto;
    }

}
