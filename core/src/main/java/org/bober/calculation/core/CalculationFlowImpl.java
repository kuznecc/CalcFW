package org.bober.calculation.core;

import org.bober.calculation.core.interfaces.CalculationFlow;
import org.springframework.context.ApplicationContext;

public class CalculationFlowImpl implements CalculationFlow{

    @Override
    public <T> T produceDto(Class<T> dtoClass, ApplicationContext appCtx) {
        ProducersContextBuilder builder = new ProducersContextBuilder(appCtx);
        T dto = builder.buildDto(dtoClass);
        return dto;
    }

}
