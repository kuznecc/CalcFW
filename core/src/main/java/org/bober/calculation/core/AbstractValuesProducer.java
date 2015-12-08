package org.bober.calculation.core;

import org.bober.calculation.core.interfaces.ValuesProducer;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractValuesProducer implements ValuesProducer {
    private Map<String, Object> result;

    protected abstract Map<String, Object> produce();

    @Override
    public Map<String, Object> getResult() {
        if (result == null) {
            result = produce();
        }
        return result;
    }

    protected static Map<String, Object> wrapSingleResult(Object object) {
        return Collections.singletonMap(RESULT, object);
    }

    protected static Map<String, Object> nullResult() {
        return Collections.singletonMap(RESULT, null);
    }
}
