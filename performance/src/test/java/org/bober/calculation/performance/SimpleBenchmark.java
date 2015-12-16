
package org.bober.calculation.performance;

import org.bober.calculation.ProductionFlow;
import org.bober.calculation.ValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.openjdk.jmh.annotations.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;

public class SimpleBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 4)
    @Fork(1)
    public Object testMethod() {
        ProductionFlow flow = new ProductionFlow();
        Dto dto = flow.produceDto(Dto.class);
        return dto;
    }

    public static class Dto {
        @ValuesProducerResult(producer = Producer.class)
        public String producerResult;
    }

    public static class Producer implements ValuesProducer {
        @ValuesProducerResult(producer = Producer1.class)
        public String externalResult;

        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) externalResult);
        }
    }

    public static class Producer1 implements ValuesProducer {
        @ValuesProducerResult(producer = Producer2.class)
        public String externalResult;

        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) externalResult);
        }
    }

    public static class Producer2 implements ValuesProducer {
        @ValuesProducerResult(producer = Producer3.class)
        public String externalResult;

        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) externalResult);
        }
    }

    public static class Producer3 implements ValuesProducer {
        @ValuesProducerResult(producer = Producer4.class)
        public String externalResult;

        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) externalResult);
        }
    }

    public static class Producer4 implements ValuesProducer {
        @Override
        public Map<String, Object> getResult() {
            return singletonMap(RESULT, (Object) "value");
        }
    }


//    Result "testMethod":
//    0,050 ±(99.9%) 0,001 ms/op [Average]
//    (min, avg, max) = (0,044, 0,050, 0,060), stdev = 0,002
//    CI (99.9%): [0,050, 0,051] (assumes normal distribution)
//
//    # Run complete. Total time: 00:06:46
//    Benchmark               Mode  Cnt  Score   Error  Units
//    MyBenchmark.testMethod  avgt  200  0,050 ± 0,001  ms/op


}
