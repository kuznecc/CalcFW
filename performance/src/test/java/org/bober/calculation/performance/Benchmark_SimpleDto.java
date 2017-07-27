
package org.bober.calculation.performance;

import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bober.calculation.CalcFlow;
import org.bober.calculation.ValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

public class Benchmark_SimpleDto {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 4)
    @Fork(1)
    public Object recursion() {
        CalcFlow flow = CalcFlow.recursive();
        Dto dto = flow.produceClass(Dto.class);
        return dto;
    }
//    Result: 0,003 ±(99.9%) 0,000 ms/op [Average]
//    Statistics: (min, avg, max) = (0,003, 0,003, 0,003), stdev = 0,000
//    Confidence interval (99.9%): [0,003, 0,003]
//    Benchmark                               Mode  Cnt  Score   Error  Units
//    GranularCalculationBenchmark.recursion  avgt   20  0,003 ± 0,000  ms/op

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 4)
    @Fork(1)
    public Object graphSingleThread() {
        CalcFlow flow = CalcFlow.recursive();
        Dto dto = flow.produceClass(Dto.class);
        return dto;
    }
//    Result: 0,008 ±(99.9%) 0,000 ms/op [Average]
//    Statistics: (min, avg, max) = (0,008, 0,008, 0,009), stdev = 0,000
//    Confidence interval (99.9%): [0,008, 0,009]
//    Benchmark                              Mode  Cnt  Score   Error  Units
//    Benchmark_SimpleDto.graphSingleThread  avgt   20  0,008 ± 0,000  ms/op

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
//    MyBenchmark.graphSingleThread  avgt  200  0,050 ± 0,001  ms/op


}
