package org.bober.calculation;

import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.junit.Test;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class InheritanceDto extends AbstractCalcFlowTest {

    private static final int VALUE = 1;

    private CalcFlow flow;

    public InheritanceDto(CalcFlow flow) {
        this.flow = flow;
    }

    @Test
    public void test_ProducersInheritanceWithInterfaceMatching() throws Exception {
        ChildDto dto = flow.produceClass(ChildDto.class);

        assertThat(dto, notNullValue());

        assertThat(dto.parentClassProducer, notNullValue());
        assertThat(dto.childClassProducer,  notNullValue());
        assertThat(dto.parentFieldProducer, notNullValue());
        assertThat(dto.childFieldProducer,  notNullValue());

        assertThat(dto.parentClassProducer, is(VALUE));
        assertThat(dto.childClassProducer,  is(VALUE));
        assertThat(dto.parentFieldProducer, is(VALUE));
        assertThat(dto.childFieldProducer,  is(VALUE));
    }


    @PrepareValuesProducer(Producer2.class)
    public static class ChildDto extends ParentDto{
        @ValuesProducerResult(producer = Producer4.class)
        public Integer childFieldProducer;
        @ValuesProducerResult(producer = P2Interface.class)
        public Integer childClassProducer;
    }

    @PrepareValuesProducer(Producer1.class)
    public static class ParentDto {
        @ValuesProducerResult(producer = Producer3.class)
        public Integer parentFieldProducer;
        @ValuesProducerResult(producer = P1Interface.class)
        public Integer parentClassProducer;
    }


    public interface P1Interface extends ValuesProducer { }

    public static class Producer1 implements P1Interface{
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) VALUE);
        }
    }

    public interface P2Interface extends ValuesProducer { }

    public static class Producer2 implements P2Interface{
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) VALUE);
        }
    }

    public static class Producer3 implements ValuesProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) VALUE);
        }
    }

    public static class Producer4 implements ValuesProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) VALUE);
        }
    }

    public static abstract class AbstractProducer implements ValuesProducer {
    }

}
