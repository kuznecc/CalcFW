package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public interface CalcFlow {
    <T> T produceClass(Class<T> dtoClass);

    <T> T produceClass(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedCalcCtx);
}
