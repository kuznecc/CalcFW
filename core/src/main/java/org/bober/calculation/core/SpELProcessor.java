package org.bober.calculation.core;

import org.bober.calculation.core.annotation.ValuesProducerResult;
import org.bober.calculation.core.interfaces.ValuesProducer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.util.Map;

import static java.util.Collections.singletonMap;


public class SpELProcessor {

    private static ExpressionParser parser = new SpelExpressionParser();

    public static boolean isItSpelOnFieldDetected(Field field) {
        ValuesProducerResult vpr = field.getAnnotation(ValuesProducerResult.class);
        return vpr != null && vpr.producer() != null && vpr.resultName() != null
                && vpr.exp() != null && !vpr.exp().equals(ValuesProducerResult.NO_EXPRESSION);
    }

    public static Object evaluateSpelExpression(Field field, Map producersCtx) {
        if (!isItSpelOnFieldDetected(field) && producersCtx == null) {
            return null;
        }

        ValuesProducerResult vpr = field.getAnnotation(ValuesProducerResult.class);
        ValuesProducer producer = (ValuesProducer) producersCtx.get(vpr.producer());
        Object result = producer.getResult().get(vpr.resultName());

        return vpr.expAlias().equals(ValuesProducerResult.NO_EXP_ALIAS)
                ? evaluateSpelExpression(result, vpr.exp())
                : evaluateSpelExpression(singletonMap(vpr.expAlias(), result), vpr.exp());
    }

    public static Object evaluateSpelExpression(Object obj, String expression) {
        return parser.parseExpression(expression).getValue(obj);
    }

    public static Object evaluateSpelExpression(Map<String, Object> objMap, String expression) {
        StandardEvaluationContext spelCtx = new StandardEvaluationContext();
        for (String alias : objMap.keySet()) {
            spelCtx.setVariable(alias, objMap.get(alias));
        }
        return parser.parseExpression(expression).getValue(spelCtx);
    }
}
