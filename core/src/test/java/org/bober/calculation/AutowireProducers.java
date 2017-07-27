package org.bober.calculation;

import org.bober.calculation.annotation.ValuesProducerResult;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class AutowireProducers extends AbstractCalcFlowTest {

    public static final Integer TEST_SIMPLE_PRODUCER_RESULT = 42;

    private CalcFlow flow;


    public AutowireProducers(CalcFlow flow) {
        this.flow = flow;
    }

        @Test
    public void test_SingleProducer() throws Exception {
        TestDto_OneProducer dto = flow.produceClass(TestDto_OneProducer.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult, notNullValue());
        assertThat(dto.producerResult, is(TEST_SIMPLE_PRODUCER_RESULT));
    }

    @Test
    public void test_TwoEqProducers() throws Exception {
        TestDto_TwoEqProducers dto = flow.produceClass(TestDto_TwoEqProducers.class);

        assertThat(dto, notNullValue());
        assertThat(dto.simpleProducerResult, notNullValue());
        assertThat(dto.anotherSimpleProducerResult, notNullValue());
        assertThat(dto.simpleProducerResult, is(TEST_SIMPLE_PRODUCER_RESULT));
        assertThat(dto.anotherSimpleProducerResult, is(TEST_SIMPLE_PRODUCER_RESULT));
    }

    @Test
    public void test_TwoDifProducers() throws Exception {
        TestDto_TwoDifProducers dto = flow.produceClass(TestDto_TwoDifProducers.class);

        assertThat(dto, notNullValue());
        assertThat(dto.simpleProducerResult, notNullValue());
        assertThat(dto.anotherSimpleProducerResult, notNullValue());
        assertThat(dto.simpleProducerResult, is(TEST_SIMPLE_PRODUCER_RESULT));
        assertThat(dto.anotherSimpleProducerResult, is(TEST_SIMPLE_PRODUCER_RESULT));
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
        @ValuesProducerResult(producer = TestProducer.class)
        public Integer producerResult;
    }

    public static class TestDto_TwoEqProducers {
        @ValuesProducerResult(producer = TestProducer.class)
        public Integer simpleProducerResult;

        @ValuesProducerResult(producer = TestProducer.class)
        public Integer anotherSimpleProducerResult;
    }

    public static class TestDto_TwoDifProducers {
        @ValuesProducerResult(producer = TestProducer.class)
        public Integer simpleProducerResult;

        @ValuesProducerResult(producer = AnotherTestProducer.class)
        public Integer anotherSimpleProducerResult;
    }

}
