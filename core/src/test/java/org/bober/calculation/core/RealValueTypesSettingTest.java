package org.bober.calculation.core;

import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.junit.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RealValueTypesSettingTest {

    public static final String TEST_STRING = "qwerty";
    public static final Integer TEST_INTEGER = 42;
    public static final Long TEST_LONG = 42222L;
    public static final LocalDate TEST_LOCAL_DATE = LocalDate.of(1999,9,9);

    private ProductionFlow flow = new ProductionFlow();

    @Test
    public void test_PassingRawValuesFromProducersToFields() throws Exception {
        Dto dto = flow.produceDto(Dto.class);

        assertThat(dto, notNullValue());
        assertThat(dto.stringResult, is(TEST_STRING));
        assertThat(dto.integerResult, is(TEST_INTEGER));
        assertThat(dto.longResult, is(TEST_LONG));
        assertThat(dto.localDateResult, is(TEST_LOCAL_DATE));
        assertThat(dto.unnecessaryValue, nullValue());
        assertThat(dto.nullValue, nullValue());
    }



    public static class Dto {
        @ValuesProducerResult(producer = Producer.class, resultName = Producer.STRING)
        public String stringResult;
        @ValuesProducerResult(producer = Producer.class, resultName = Producer.INTEGER)
        public Integer integerResult;
        @ValuesProducerResult(producer = Producer.class, resultName = Producer.LONG)
        public Long longResult;
        @ValuesProducerResult(producer = Producer.class, resultName = Producer.LOCAL_DATE)
        public LocalDate localDateResult;
        @ValuesProducerResult(producer = Producer.class, resultName = Producer.UNNECESSARY, required = false)
        public Object unnecessaryValue;
        @ValuesProducerResult(producer = Producer.class, resultName = Producer.NULL_VALUE)
        public String nullValue;
    }


    public static class Producer implements ValuesProducer {
        public static final String STRING = "string";
        public static final String INTEGER = "integer";
        public static final String LONG = "long";
        public static final String LOCAL_DATE = "localDate";
        public static final String UNNECESSARY = "unnecessary";
        public static final String NULL_VALUE = "nullValue";

        @Override
        public Map<String, Object> getResult() {
            HashMap result = new HashMap<>();
            result.put(STRING, TEST_STRING);
            result.put(INTEGER, TEST_INTEGER);
            result.put(LONG, TEST_LONG);
            result.put(LOCAL_DATE, TEST_LOCAL_DATE);
            result.put(NULL_VALUE, null);
            return result;
        }
    }

}
