package org.bober.calculation;

import java.util.Map;

import org.springframework.context.ApplicationContext;

public interface CalcContextBuilder {
    <T> T buildClass(Class<T> clazz, ApplicationContext springAppCtx, Map<Class, Object> preparedProducersCtx);
}
