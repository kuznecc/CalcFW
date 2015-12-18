package org.bober.calculation;

import org.bober.calculation.annotation.ValuesProducerResult;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ProducersWithPreparedContext extends AbstractProductionFlowTest{

    public static final String TEST_VALUE = "value";

    private ProductionFlow flow;

    public ProducersWithPreparedContext(ProductionFlow flow) {
        this.flow = flow;
    }

    @Test
    public void test_ComplexProducer() throws Exception {
        Map<Class, Object> ctx = new HashMap<>();
        ctx.put(Contaner.class, new Contaner(TEST_VALUE));

        TestDto dto = flow.produceClass(TestDto.class, null, ctx);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult, is(TEST_VALUE));
    }

    public static class Contaner extends AbstractValuesProducer {
        private String val;

        public Contaner(String val) {
            this.val = val;
        }

        @Override
        protected Map<String, Object> produce() {
            return wrapSingleResult(val);
        }
    }

    public static class TestDto {
        @ValuesProducerResult(producer = Producer.class)
        public String producerResult;
    }


    public static class Producer implements ValuesProducer {
        @ValuesProducerResult(producer = Contaner.class)
        private String preparedValue;

        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) preparedValue);
        }
    }

}
