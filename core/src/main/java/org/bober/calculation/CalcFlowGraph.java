package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public class CalcFlowGraph implements CalcFlow {

    @Override
    public <T> T produceClass(Class<T> dtoClass){
        return produceClass(dtoClass, null, null);
    }

    @Override
    public <T> T produceClass(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedCalcCtx) {
        CalcContextBuilder builder = new CalcContextBuilderGraph(appCtx);
        T dto = builder.buildClass(dtoClass, preparedCalcCtx);
        return dto;
    }

}
