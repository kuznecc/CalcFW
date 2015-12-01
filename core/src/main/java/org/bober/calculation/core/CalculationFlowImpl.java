package org.bober.calculation.core;

import org.bober.calculation.core.interfaces.CalculationFlow;

public class CalculationFlowImpl implements CalculationFlow{

    @Override
    public <T> T produceDto(Class<T> dtoClass) {
        T dto = ProducersContextBuilder.buildDto(dtoClass);
        return dto;
    }

}
