package org.bober.calculation.core;

import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.CalculationFlow;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ProducersWithSpringDependencies.SpringConfig.class)
public class ProducersWithSpringDependencies {

    public static final String BEAN_NAME = "beanName";
    public static final String TEST_VALUE = "value";

    @Autowired
    private ApplicationContext appCtx;

    private CalculationFlow flow = new CalculationFlowImpl();

    @Test
    public void test_ComplexProducer() throws Exception {
        TestDto dto = flow.produceDto(TestDto.class, appCtx);

        assertThat(dto, notNullValue());
        assertThat(dto.producerResult, is(TEST_VALUE));
    }


    public static class TestDto {

        @ValuesProducerResult(producer = Producer.class)
        public String producerResult;

    }


    public static class Producer implements ValuesProducer {
        @Autowired
        @Qualifier(BEAN_NAME)
        private String valueSource;

        @Override
        public Map<String, Object> getResult() {
            return Collections.singletonMap(RESULT, (Object) valueSource);
        }
    }

    @Configuration
    static class SpringConfig {
        @Bean(name = BEAN_NAME)
        public String testBean() {
            return TEST_VALUE;
        }
    }

}
