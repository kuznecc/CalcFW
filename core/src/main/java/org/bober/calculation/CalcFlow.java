package org.bober.calculation;

import org.bober.calculation.impl.CalcContextBuilderGraph;
import org.bober.calculation.impl.CalcContextBuilderParallelRecursion;
import org.bober.calculation.impl.CalcContextBuilderRecursion;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class CalcFlow {


    public static CalcFlow recursive() {
        return new CalcFlow(CalcContextBuilderRecursion.class);
    }

    public static CalcFlow graph() {
        return new CalcFlow(CalcContextBuilderGraph.class);
    }

    public static CalcFlow parallelRecursion() {
        return new CalcFlow(CalcContextBuilderParallelRecursion.class);
    }


    private Class<? extends CalcContextBuilder> calcContextBuilderClass;

    private CalcFlow(Class<? extends CalcContextBuilder> calcContextBuilderClass) {
        this.calcContextBuilderClass = calcContextBuilderClass;
    }

    public <T> T produceClass(Class<T> dtoClass, ApplicationContext springAppCtx, Map<Class, Object> preparedCalcCtx)
            throws IllegalAccessException, InstantiationException {
        return calcContextBuilderClass.newInstance().buildClass(dtoClass, springAppCtx, preparedCalcCtx);
    }

    public <T> T produceClass(Class<T> dtoClass) throws InstantiationException, IllegalAccessException {
        return produceClass(dtoClass, null, null);
    }
}
