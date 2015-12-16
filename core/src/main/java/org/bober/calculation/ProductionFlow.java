package org.bober.calculation;

import org.springframework.context.ApplicationContext;

import java.util.Map;

public interface ProductionFlow {
    <T> T produceClass(Class<T> dtoClass);

    <T> T produceClass(Class<T> dtoClass, ApplicationContext appCtx, Map<Class, Object> preparedProductionCtx);
}
