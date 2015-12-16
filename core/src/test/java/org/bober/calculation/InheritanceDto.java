package org.bober.calculation;

import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.junit.Test;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class InheritanceDto {

    public static int index = 1;

    private ProductionFlow flow = new RecursionProductionFlow();

    @Test
    public void test_SequenceOfProducerInstantiation() throws Exception {
        ChildDto dto = flow.produceClass(ChildDto.class);

        assertThat(dto, notNullValue());

        assertThat(dto.parentClassProducer, notNullValue());
        assertThat(dto.childClassProducer,  notNullValue());
        assertThat(dto.parentFieldProducer, notNullValue());
        assertThat(dto.childFieldProducer,  notNullValue());

        assertThat(dto.parentClassProducer, is(1));
        assertThat(dto.childClassProducer,  is(2));
        assertThat(dto.parentFieldProducer, is(3));
        assertThat(dto.childFieldProducer,  is(4));
    }


    @PrepareValuesProducer(Producer2.class)
    public static class ChildDto extends ParentDto{
        @ValuesProducerResult(producer = Producer4.class)
        public Integer childFieldProducer;
        @ValuesProducerResult(producer = Producer2.class)
        public Integer childClassProducer;
    }

    @PrepareValuesProducer(Producer1.class)
    public static class ParentDto {
        @ValuesProducerResult(producer = Producer3.class)
        public Integer parentFieldProducer;
        @ValuesProducerResult(producer = Producer1.class)
        public Integer parentClassProducer;
    }


    public static class Producer1 extends AbstractProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) val);
        }
    }

    public static class Producer2 extends AbstractProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) val);
        }
    }

    public static class Producer3 extends AbstractProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) val);
        }
    }

    public static class Producer4 extends AbstractProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) val);
        }
    }

    public static abstract class AbstractProducer implements ValuesProducer {
        protected Integer val = index++;
    }

}
