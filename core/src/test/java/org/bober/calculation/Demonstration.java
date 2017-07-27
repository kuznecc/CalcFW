package org.bober.calculation;


import org.bober.calculation.annotation.PrepareValuesProducer;
import org.bober.calculation.annotation.ValuesProducerResult;

import java.util.HashMap;
import java.util.Map;

public class Demonstration {

    public static void main(String[] args) {
        CalcFlow flow = new CalcFlowRecursion();
        GuitaristDto guitarist = flow.produceClass(GuitaristDto.class);
        DrummerDto drummer = flow.produceClass(DrummerDto.class);
        System.out.println();
    }


    @PrepareValuesProducer(MusicalInstrumentDrums.class)
    public static class DrummerDto extends MusicianDto {
        @ValuesProducerResult(producer = DrummerHitsPerSecond.class, expAlias = "h", exp = "#h * 60")
        Integer hitsPerMinute;
    }


    @PrepareValuesProducer(MusicalInstrumentGuitar.class)
    public static class GuitaristDto extends MusicianDto {
        @ValuesProducerResult(producer = GuitarEffectsPedal.class, resultName = GuitarEffectsPedal.BOSS)
        String effectsPedal;
    }


    public static class MusicianDto {
        @ValuesProducerResult(producer = MusicalInstrumentProducer.class, exp = "toUpperCase()")
        String instrument;
    }


    public static class DrummerHitsPerSecond extends AbstractValuesProducer{
        @Override
        protected Map<String, Object> produce() {
            return wrapSingleResult(3);
        }
    }

    public static class GuitarEffectsPedal extends AbstractValuesProducer {
        public static final String ELECTRO_HARMONIX = "Electro-Harmonix";
        public static final String BOSS = "boss";
        @Override
        protected Map<String, Object> produce() {
            HashMap<String, Object> result = new HashMap<>();
            result.put(ELECTRO_HARMONIX, "Electro-Harmonix B9");
            result.put(BOSS, "LiKe A BOSS M100500");
            return result;
        }
    }


    public static class MusicalInstrumentGuitar extends AbstractValuesProducer implements MusicalInstrumentProducer {
        @Override
        protected Map<String, Object> produce() {
            return wrapSingleResult("Guitar");
        }
    }


    public static class MusicalInstrumentDrums extends AbstractValuesProducer implements MusicalInstrumentProducer {
        @Override
        protected Map<String, Object> produce() {
            return wrapSingleResult("drums");
        }
    }


    public static interface MusicalInstrumentProducer extends ValuesProducer {
    }

}
