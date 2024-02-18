# Actions

Developer-oriented documentation for actions.

## Action API

The workflow engine of the LASSO platform is based on the principle of _Inversion of Control_ (IoC) as known from popular frameworks like Spring.

General structure of a Java Action class

```java
@LassoAction(desc = "An action with no behaviour")
public class NoOp extends DefaultAction {

    @LassoInput(desc = "a configuration parameter", optional = true)
    public String paramExample;

    @Override
    public void execute(LSLExecutionContext ctx, ActionConfiguration conf) throws IOException {
        // abstraction container (SM)
        Abstraction abstraction = conf.getAbstraction();
    }
}
```

Every Java Action has to inherit from a certain subclass from the Action API (here the abstract class `DefaultAction`). In addition to object-oriented inheritance of well-defined lifecycle methods that are used by the workflow engine, the Action class implementation is further described to the workflow engine by using a certain set of mandatory and option annotation classes like `@LassoInput` for marking class fields as LSL action configuration parameters. Moreover, a Java Action class can describe itself using the marker annotation `@LassoAction`.

Once the Java Action class is known to the workflow engine, it can be used as part of LSL pipeline script executions as follows

```groovy
action(name:'noOp',type:'NoOp') {
    paramExample = 'hello world'

    dependsOn '...'
    includeAbstractions '...'

    whenAbstractionsReady() { ... }
}
```

## Docker images for Actions

LASSO Actions may require additional tools/techniques to be present. A common approach to integrate external tools/technique is to use  [docker.md](docker.md).