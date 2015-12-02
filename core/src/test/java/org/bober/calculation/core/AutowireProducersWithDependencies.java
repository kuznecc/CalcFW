package org.bober.calculation.core;

import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.CalculationFlow;
import org.bober.calculation.core.interfaces.ProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.bober.calculation.core.interfaces.ValuesProducer.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class AutowireProducersWithDependencies {

    public static final Integer TEST_VALUE = 42;

    private CalculationFlow flow = new CalculationFlowImpl();

    @Test
    public void test_ProducerSourceReturnGoodValue() throws Exception {
        TestValueSource.VALUE = TEST_VALUE;
        TestDto_ProducerWithDepended dto = flow.produceDto(TestDto_ProducerWithDepended.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult.get(), notNullValue());
        assertThat(dto.producerResult.get(), is(TEST_VALUE));
    }

    @Test
    public void test_ProducerSourceReturnNull() throws Exception {
        TestValueSource.VALUE = null;
        TestDto_ProducerWithDepended dto = flow.produceDto(TestDto_ProducerWithDepended.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult.get(), nullValue());
    }

    @Test
    public void test_ProducerCrossDependency() throws Exception {
        // complexP() = p1() + p2(P1)
        TestValueSource.VALUE = TEST_VALUE;
        TestDto_ProducerCrossDependency dto = flow.produceDto(TestDto_ProducerCrossDependency.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult.get(), is(TEST_VALUE*2));
    }



    public static class TestDto_ProducerCrossDependency {
        @ValuesProducerResult(producer = ProducerCrossDepended.class, resultName = RESULT)
        public ProducerResult<Integer> producerResult;
    }

    public static class ProducerCrossDepended implements ValuesProducer {
        @ValuesProducerResult(producer = TestProducer.class, resultName = RESULT)
        private ProducerResult<Integer> testProducer;
        @ValuesProducerResult(producer = TestValueSource.class, resultName = RESULT)
        private ProducerResult<Integer> valueSource;

        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) (valueSource.get() + testProducer.get()));
        }

    }

    public static class TestDto_ProducerWithDepended {
        @ValuesProducerResult(producer = TestProducer.class, resultName = RESULT)
        public ProducerResult<Integer> producerResult;
    }

    public static class TestProducer implements ValuesProducer {
        @ValuesProducerResult(producer = TestValueSource.class, resultName = RESULT)
        private ProducerResult<Integer> valueSource;

        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) valueSource.get());
        }
    }


    public static class TestValueSource implements ValuesProducer {
        public static Integer VALUE;
        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object)VALUE);
        }
    }

}
