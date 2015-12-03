package org.bober.calculation.core;

import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.junit.Test;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SpELProducerResultPostProcessing {
    public static final String TEST_VALUE = "testValue";

    private CalculationFlow flow = new CalculationFlow();

    @Test
    public void test_SequenceOfProducerInstantiation() throws Exception {
        Dto dto = flow.produceDto(Dto.class);

        assertThat(dto, notNullValue());
        assertThat(dto.rawResult, is(TEST_VALUE));
        assertThat(dto.upperCaseResult, equalTo(TEST_VALUE.toUpperCase()));
        assertThat(dto.concatenatedResult, equalTo(TEST_VALUE + TEST_VALUE));
    }

    public static class Dto {
        @ValuesProducerResult(producer = Producer.class)
        public String rawResult;
        @ValuesProducerResult(producer = Producer.class, exp = "toUpperCase()")
        public String upperCaseResult;
        @ValuesProducerResult(producer = Producer.class, exp = "#s + #s", expAlias = "s")
        public String concatenatedResult;
    }

    public static class Producer implements ValuesProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) TEST_VALUE);
        }
    }

}
