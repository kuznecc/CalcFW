package org.bober.calculation;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(value = Parameterized.class)
public abstract class AbstractProductionFlowTest {

    @Parameterized.Parameters
    public static Collection<ProductionFlow[]> flows() {
        return Stream
                .of(
                        new ProductionFlowRecursion()
                        , new ProductionFlowGraph()
                        , new ProductionFlowRecursionMultiThread()
//                        , new ProductionFlowGraphMultiThread()
                )
                .map(AbstractProductionFlowTest::toArray)
                .collect(Collectors.toList());
    }

    public static <T> T[] toArray(T a) {
        T[] arr = (T[]) Array.newInstance(a.getClass(), 1);
        arr[0] = a;
        return arr;
    }

}
