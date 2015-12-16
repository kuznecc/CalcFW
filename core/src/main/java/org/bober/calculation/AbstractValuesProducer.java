package org.bober.calculation;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractValuesProducer implements ValuesProducer {
    private static final Map<String, Object> NULL_SINGLE_RESULT = Collections.singletonMap(RESULT, null);

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
        return NULL_SINGLE_RESULT;
    }
}
