package org.bober.calculation;

import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class OnDtoClassProducers extends AbstractProductionFlowTest{

    public static final Integer TEST_VALUE = 42;

    private ProductionFlow flow;

    public OnDtoClassProducers(ProductionFlow flow) {
        this.flow = flow;
    }

    @Test
    public void test_ComplexProducer() throws Exception {
        TestDto dto = flow.produceClass(TestDto.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult, is(TEST_VALUE));
    }



    @PrepareValuesProducer(ValueSourceImpl.class)
    public static class TestDto {
        @ValuesProducerResult(producer = Producer.class)
        public Integer producerResult;
    }


    public static class Producer implements ValuesProducer {
        @ValuesProducerResult(producer = ValueSource.class)
        private Integer valueSource;

        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) valueSource);
        }
    }


    public static class ValueSourceImpl implements ValueSource {
        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) TEST_VALUE);
        }
    }

    public interface ValueSource extends ValuesProducer {
    }

}
