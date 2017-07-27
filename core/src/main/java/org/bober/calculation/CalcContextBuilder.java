package org.bober.calculation;

import java.util.Map;

public interface CalcContextBuilder {
    <T> T buildClass(Class<T> clazz, Map<Class, Object> preparedProducersCtx);
}
