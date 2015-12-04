/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.bober.calculation.performance;

import org.bober.calculation.core.CalculationFlow;
import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.openjdk.jmh.annotations.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;

public class SimpleBenchmark {

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
//    @Warmup(iterations = 4)
//    @Fork(1)
    public Object testMethod() {
        // This is a demo/sample template for building your JMH benchmarks. Edit as needed.
        // Put your benchmark code here.
        CalculationFlow flow = new CalculationFlow();
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
