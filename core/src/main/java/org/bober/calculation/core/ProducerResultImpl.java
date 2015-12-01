package org.bober.calculation.core;

import org.bober.calculation.core.interfaces.ProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.springframework.util.Assert;

public class ProducerResultImpl<T> implements ProducerResult<T>{
    private ValuesProducer producer;
    private String resultName;

    public ProducerResultImpl(ValuesProducer producer, String resultName) {
        Assert.notNull(producer);
        Assert.notNull(resultName);
        this.producer = producer;
        this.resultName = resultName;
    }

    @Override
    public T get() {
        if (!producer.isItCalculated()) {
            producer.calculate();
        }
        return (T) producer.getResult().get(resultName);
    }
}
