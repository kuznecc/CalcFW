

###Main classes:

- `interface ValuesProducer` - common interface for all producers that used for their behavior
- `class AbstractValuesProducer` - implementation of common logic for all producers.
    You need to extend this class to implement your own producers.
- `@ValuesProducerResult` - this annotation used for binding of producer result to some class field.
- `@PrepareValuesProducer` - you will need this annotation only if you want to pass in the same
    'producer A' field results of different 'producer B' implementation.
- `class ProductionFlow` - entry point of framework.

###Abstraction

- **Producer** - object that can produce some result that which represented in the
    Map<String, Object>, where key it's constant name of result. Producers that produce only one value
    should use default result name from interface.
- **ProducersContext** - It's `Map<Class, Object>` that contain instances of all founded producers.

###How it works:
1. Pass root class to `ProductionFlow` for further processing.
2. Process on class annotation `@PrepareValuesProducer`.  
  - Collect producers that mentioned in this annotation from this class and they parent classes,
    instantiate it all and put to producers context. Instantiation of this producers will performed
    as it described in steps 3-5 .
3. Make instantiate of specified class via default constructor.
  - If you've passed Spring Application Context to `ProductionFlow` than instance will be created
    via BeanFactory from it. So due instantiation process all Spring annotations (like`@Autowire`) will be
    processed and you will got fully wired object.
  - If you aren't pass Spring context to ProductionFlow than instantiation of class will be performed
    straightway via java reflection.
4. Wire instance fields with producer results. Repeat this steps for each annotated field :
  - Iterate instance class fields for annotation `@ValuesProducerResult`
  - Seek in *producersContext* for instance of mentioned in annotation producer. If it doesn't exist than
    execute steps 3-5 for mentioned in annotation producer class.
  - Get instance of mentioned producer from *producersContext* and get needed result from it.
  - If `@ValuesProducerResult` have some SpEL expression than producer result, alias and expression will
    passed to SpELProcessor.
  - Set result to field.
5. Put fully prepared instance into ProducersContext

###Examples:

[Demonstration class](org.bober.calculation.core.Demonstration.java)
https://github.com/kuznecc/GranularCalculationFramework/blob/master/core/src/test/java/org/bober/calculation/core/Demonstration.java