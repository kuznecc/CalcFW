package org.bober.calculation;

import org.bober.calculation.annotation.ValuesProducerResult;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class SpELProducerResultPostProcessing {
    public static final String TEST_VALUE = "testValue";
    public static final String NULL_RESULT = "null";
    private ProductionFlow flow = new ProductionFlow();

    @Test
    public void test_SequenceOfProducerInstantiation() throws Exception {
        Dto dto = flow.produceDto(Dto.class);

        assertThat(dto, notNullValue());
        assertThat(dto.rawResult, is(TEST_VALUE));
        assertThat(dto.upperCaseResult, equalTo(TEST_VALUE.toUpperCase()));
        assertThat(dto.concatenatedResult, equalTo(TEST_VALUE + TEST_VALUE));
        assertThat(dto.nullResult, nullValue());
    }

    public static class Dto {
        @ValuesProducerResult(producer = Producer.class)
        public String rawResult;
        @ValuesProducerResult(producer = Producer.class, exp = "toUpperCase()")
        public String upperCaseResult;
        @ValuesProducerResult(producer = Producer.class, exp = "#s + #s", expAlias = "s")
        public String concatenatedResult;
        @ValuesProducerResult(producer = Producer.class, resultName = NULL_RESULT, exp = "toString()", required = false)
        public String nullResult;
    }

    public static class Producer implements ValuesProducer {
        @Override
        public Map<String, Object> getResult() {
            HashMap<String, Object> result = new HashMap<>();
            result.put(RESULT, TEST_VALUE);
            result.put(NULL_RESULT, null);
            return result;
        }
    }

}
