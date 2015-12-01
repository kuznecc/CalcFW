package org.bober.calculation.core;

import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.CalculationFlow;
import org.bober.calculation.core.interfaces.ProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class AutowireProducers {

    public static final Integer TEST_SIMPLE_PRODUCER_RESULT = 42;

    private CalculationFlow flow = new CalculationFlowImpl();

    @Test
    public void test_SingleProducer() throws Exception {
        TestDto_OneProducer dto = flow.produceDto(TestDto_OneProducer.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult.get(), notNullValue());
        assertThat(dto.producerResult.get(), is(TEST_SIMPLE_PRODUCER_RESULT));
    }

    @Test
    public void test_TwoEqProducers() throws Exception {
        TestDto_TwoEqProducers dto = flow.produceDto(TestDto_TwoEqProducers.class);

        assertThat(dto, notNullValue());
        assertThat(dto.simpleProducerResult.get(), notNullValue());
        assertThat(dto.anotherSimpleProducerResult.get(), notNullValue());
        assertThat(dto.simpleProducerResult.get(), is(TEST_SIMPLE_PRODUCER_RESULT));
        assertThat(dto.anotherSimpleProducerResult.get(), is(TEST_SIMPLE_PRODUCER_RESULT));
    }

    @Test
    public void test_TwoDifProducers() throws Exception {
        TestDto_TwoDifProducers dto = flow.produceDto(TestDto_TwoDifProducers.class);

        assertThat(dto, notNullValue());
        assertThat(dto.simpleProducerResult.get(), notNullValue());
        assertThat(dto.anotherSimpleProducerResult.get(), notNullValue());
        assertThat(dto.simpleProducerResult.get(), is(TEST_SIMPLE_PRODUCER_RESULT));
        assertThat(dto.anotherSimpleProducerResult.get(), is(TEST_SIMPLE_PRODUCER_RESULT));
    }

    public static class TestProducer implements ValuesProducer {
        public static final String RESULT = "result";
        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) TEST_SIMPLE_PRODUCER_RESULT);
        }
    }

    public static class AnotherTestProducer implements ValuesProducer {
        public static final String RESULT = "result";
        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) TEST_SIMPLE_PRODUCER_RESULT);
        }
    }

    public static class TestDto_OneProducer {
        @ValuesProducerResult(producer = TestProducer.class, resultName = TestProducer.RESULT)
        public ProducerResult<Integer> producerResult;
    }

    public static class TestDto_TwoEqProducers {
        @ValuesProducerResult(producer = TestProducer.class, resultName = TestProducer.RESULT)
        public ProducerResult<Integer> simpleProducerResult;

        @ValuesProducerResult(producer = TestProducer.class, resultName = TestProducer.RESULT)
        public ProducerResult<Integer> anotherSimpleProducerResult;
    }

    public static class TestDto_TwoDifProducers {
        @ValuesProducerResult(producer = TestProducer.class, resultName = TestProducer.RESULT)
        public ProducerResult<Integer> simpleProducerResult;

        @ValuesProducerResult(producer = AnotherTestProducer.class, resultName = AnotherTestProducer.RESULT)
        public ProducerResult<Integer> anotherSimpleProducerResult;
    }

}
