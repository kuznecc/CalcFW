package org.bober.calculation;

import org.bober.calculation.annotation.ValuesProducerResult;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class AutowireProducersWithDependencies extends AbstractProductionFlowTest{

    public static final Integer TEST_VALUE = 42;

    private ProductionFlow flow;

    public AutowireProducersWithDependencies(ProductionFlow flow) {
        this.flow = flow;
    }

    @Test
    public void test_ProducerSourceReturnGoodValue() throws Exception {
        TestValueSource.VALUE = TEST_VALUE;
        TestDto_ProducerWithDepended dto = flow.produceClass(TestDto_ProducerWithDepended.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult, notNullValue());
        assertThat(dto.producerResult, is(TEST_VALUE));
    }

    @Test
    public void test_ProducerSourceReturnNull() throws Exception {
        TestValueSource.VALUE = null;
        TestDto_ProducerWithDepended dto = flow.produceClass(TestDto_ProducerWithDepended.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult, nullValue());
    }

    @Test
    public void test_ProducerCrossDependency() throws Exception {
        // complexP() = p1() + p2(P1)
        TestValueSource.VALUE = TEST_VALUE;
        TestDto_ProducerCrossDependency dto = flow.produceClass(TestDto_ProducerCrossDependency.class);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult, is(TEST_VALUE*2));
    }



    public static class TestDto_ProducerCrossDependency {
        @ValuesProducerResult(producer = ProducerCrossDepended.class)
        public Integer producerResult;
    }

    public static class ProducerCrossDepended implements ValuesProducer {
        @ValuesProducerResult(producer = TestProducer.class)
        private Integer testProducer;
        @ValuesProducerResult(producer = TestValueSource.class)
        private Integer valueSource;

        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) (valueSource + testProducer));
        }

    }

    public static class TestDto_ProducerWithDepended {
        @ValuesProducerResult(producer = TestProducer.class, required = false)
        public Integer producerResult;
    }

    public static class TestProducer implements ValuesProducer {
        @ValuesProducerResult(producer = TestValueSource.class, required = false)
        private Integer valueSource;

        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) valueSource);
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
