
package org.bober.calculation.performance;

import org.bober.calculation.*;
import org.bober.calculation.annotation.ValuesProducerResult;
import org.openjdk.jmh.annotations.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Benchmark_DelayedProducers {

    private static final int DELAY = 0;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 4)
    @Fork(1)
    public Object recursion() {
        ProductionFlow flow = new ProductionFlowRecursion();
        Dto dto = flow.produceClass(Dto.class);
        return dto;
    }
//    Benchmark                             Mode  Cnt  Score   Error  Units
//    Benchmark_DelayedProducers.recursion  avgt   20  0,021 ± 0,001  ms/op     delay=0
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 4)
    @Fork(1)
    public Object recursionMultiThread() {
        ProductionFlow flow = new ProductionFlowRecursionMultiThread();
        Dto dto = flow.produceClass(Dto.class);
        return dto;
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 4)
    @Fork(1)
    public Object graphSingleThread() {
        ProductionFlowGraph flow = new ProductionFlowGraph();
        Dto dto = flow.produceClass(Dto.class);
        return dto;
    }
//    Benchmark                             Mode  Cnt  Score   Error  Units
//    Benchmark_DelayedProducers.graph      avgt   20  0,033 ± 0,001  ms/op     delay=0

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 4)
    @Fork(1)
    public Object graphMultiThread() {
        ProductionFlow flow = new ProductionFlowGraphMultiThread();
        Dto dto = flow.produceClass(Dto.class);
        return dto;
    }

//    Result: 5,835 ±(99.9%) 8,162 ms/op [Average]
//    Statistics: (min, avg, max) = (0,019, 5,835, 35,436), stdev = 9,399
//    Confidence interval (99.9%): [-2,326, 13,997]
//    Benchmark                                    Mode  Cnt  Score   Error  Units
//    Benchmark_DelayedProducers.graphMultiThread  avgt   20  5,835 ± 8,162  ms/op


    public static class Dto {
        @ValuesProducerResult(producer = P1.class)
        public String p1;
        @ValuesProducerResult(producer = P2.class)
        public String p2;
        @ValuesProducerResult(producer = P3.class)
        public String p3;
        @ValuesProducerResult(producer = P4.class)
        public String p4;
    }

    public static class P1 extends P {
        @ValuesProducerResult(producer = P5.class)
        public String externalResult;
    }

    public static class P2 extends P {
        @ValuesProducerResult(producer = P8.class)
        public String externalResult;
    }

    public static class P3 extends P {
        @ValuesProducerResult(producer = P6.class)
        public String externalResult;
    }

    public static class P4 extends P {
    }

    public static class P5 extends P {
        @ValuesProducerResult(producer = P2.class)
        public String externalResult2;
        @ValuesProducerResult(producer = P6.class)
        public String externalResult6;
        @ValuesProducerResult(producer = P9.class)
        public String externalResult9;
        @ValuesProducerResult(producer = P7.class)
        public String externalResult7;
    }

    public static class P6 extends P {
    }

    public static class P7 extends P {
        @ValuesProducerResult(producer = P8.class)
        public String externalResult;
    }

    public static class P8 extends P {
    }

    public static class P9 extends P {
    }


    public static abstract class P extends AbstractValuesProducer {
        @Override
        protected Map<String, Object> produce() {
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return wrapSingleResult(this.getClass().getSimpleName());
        }
    }
}
