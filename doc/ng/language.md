# LASSO Scripting Language

## Anotomy of an LSL Script

```groovy
// HEADER: global study configuration
dataSource 'XXX' // global data source of executable corpus (Solr code index, Nexus artifact repository)

// global fields ...

// STUDY BLOCK
study(name: 'StudyName') {
    // one or more ACTION BLOCKS, study configuration (e.g., loading of benchmarks) etc.
    action(name: "ActionName") {
        // ...
    }
}
```

## Actions

### Types

#### Without Java Action class counterpart

```groovy
action(name: "ActionName") {
    //...
}
```

#### With Java Action class counterpart

```groovy
action(name: "ActionName", type: "ActionType") {
    //...
}
```

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

Note that even pure LSL actions have a Java Action class counterpart (i.e., __type: 'NoOp'__)

### Lifecycle

```groovy
action(name: "ActionName", type: "ActionType") {
    configure {
        // (1) configure block is called to configure the Java Action class counterpart
        // Note: typically only needed for LSL actions with an LSL Action class counterpart
    }
    
    execute {
        // (2) execute LSL commands BEFORE the Java Action class is executed
        // Note: typically only needed for LSL actions without an LSL Action class counterpart
    }

    analyze() {
        // (3) analyze results directly after execution of the Java Action class
    }
}
```

### Configuration

#### Configuration of Java Action Class

##### Using Configure Block

```groovy
action(name: "ActionName", type: "ActionType") {
    configure {
        // (1) configure block is called to configure the Java Action class counterpart
        mySetting = "myvalue"
    }
}
```

##### Using Properties

```groovy
action(name: "ActionName", type: "ActionType") {
    mySetting = "myvalue"
    // ...
}
```

#### Configuration of Pipeline (Execution DAG/Dependency DAG)

```groovy
action(name: "ActionName", type: "ActionType") {
    dependsOn 'actionName' // action filter: specify on which other actions this action depends
    include '*' // stimulus matrix filter: specify which stimulus matrices to filter (by name of the stimulus matrix; i.e., abstraction name)

    // TODO filter stimulus matrix
    includeTests '*'
    includeImplementations {name -> // closure 
        stimulusMatrices[name].implementations?.findAll { impl -> impl.id == 'XXX'} // return filtered list
    }
    
    // include { stimulusMatrix -> {...}} // TODO by stimulus matrix
}
```

## Profiles for Dynamic Analysis (Run-time environment & analysis scope)

Profiles can be declared within a study block or as part of the configuration block of an action.

### Global (Reusable) Profiles

e.g., setting Java JDK 17.

```groovy
// profile for execution
profile('java17Profile') {
    scope('class') { type = 'class' }
    environment('java17') {
        image = 'maven:3.9-eclipse-temurin-17'
    }
}
```

Referencing study profile in action

```groovy
action(name: "ActionName") {
    //...
    profile('java17Profile') // reference by name
}
```

### Local Profile

e.g., setting Java JDK 17.

```groovy
action(name: "ActionName") {
    //...
    profile('java17Profile') {
        scope('class') { type = 'class' }
        environment('java17') {
            image = 'maven:3.9-eclipse-temurin-17'
        }
    }
}
```

## Resuming Studies at some point / Referencing other Studies

```groovy
action(name: "ActionName") {
    dependsOn 'executionId:actionName' // URI: LSL script execution id ":" action name
    include '*' // specify which stimulus matrices to filter
    
    // ...
}
```

## Benchmarks

Loading benchmarks integrated into the LASSO platform.

### Within Study Block

```groovy
def humanEval = loadBenchmark("humaneval-java-reworded")
```

### Within Action Block

```groovy
action(name: "ActionName") {
    def humanEval = loadBenchmark("humaneval-java-reworded")
    //...
}
```

## Creating stimulus matrices

```groovy
action(name: 'createStimulusMatrix') {
    execute {
        // create new stimulus matrix: abstraction name, interface specification in LQL, list of implementations, list of tests
        stimulusMatrix('AbstractionName', 'LQL', [/*implementations*/], [/*tests*/])
    }
}
```

### Adding implementations

#### Known (Maven) Artifacts

```groovy
// command
implementation("ID", "CLASSNAME", "MAVENCOORDINATES")

// example
implementation("1", "org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15")
```

#### JDK Classes

```groovy
// command
implementation("ID", "CLASSNAME")

// example
implementation("3", "java.util.LinkedList")
```

#### Code Search

##### Interface-Driven Code Search (LQL)

```groovy
/* query implementation candidates using interface-driven code search via LQL */
action(name: 'select', type: 'Search') {
    // ...
    
    query { stimulusMatrix ->
        def query = [:] // create query model
        query.queryContent = stimulusMatrix.lql
        query.rows = 10
        query.dataSource = "XXX" // optional: may override global data source
        query.filters = [/*query filters*/]
        return [query] // list of queries is expected
    }
}
```

#### Code Generation

```groovy
action(name: 'generateCodeLlama', type: 'GenerateCodeOllama') {
    // ...

    // custom DSL command offered by the action (for each stimulus matrix, create prompts to obtain implementations)
    prompt { stimulusMatrix ->
        // can by for any prompts: FA, impls, models etc.
        def prompt = [:] // create prompt model
        prompt.promptContent = """implement a java class with the following interface specification, but do not inherit a java interface: ```${stimulusMatrix.lql}```. Only output the java class and nothing else."""
        prompt.id = "lql_prompt"
        return [prompt] // list of prompts is expected
    }
}
```

Generate for each implementation

```groovy
prompt { stimulusMatrix ->
    List prompts = stimulusMatrix.implementations.collect { impl ->
        def prompt = [:] // create prompt model
        prompt.promptContent = """generate a diverse variant of the following code unit: ```${impl.code.content}```. Only output the variant class and nothing else."""
        prompt.id = "lql_prompt"
        prompt.model = "llama3.1:latest"
        return prompt
    }

    return prompts
}
```

#### Individual

Implementations can be combined ... .

### Adding Tests

#### Manual Tests in LSL

```groovy
test(name: 'testPush()') {
    row '',  'create', 'Stack'
    row '',  'push',   'A1',     '"Hi"'
    row '',  'size',   'A1'
}

test(name: 'testPushParameterized(p1=java.lang.String)', p1: "Hello World!") {
    row '',  'create', 'Stack'
    row '',  'push',   'A1',     '?p1'
    row '',  'size',   'A1'
}

test(name: 'testPushParameterized(p1=java.lang.String)', p1: "Bla blub!") // e.g., parameterized
```

#### Existing Tests from Benchmarks

HumanEval, mbpp from MultiPL-E.

```groovy
// load benchmark
def humanEval = loadBenchmark("humaneval-java-reworded")

action(name: "createStimulusMatrices") {
    execute {
        // create stimulus matrices for given problems
        def myProblems = [humanEval.abstractions['HumanEval_13_greatest_common_divisor']]
        myProblems.each { problem ->
            stimulusMatrix(problem.id, problem.lql, [/*impls*/], problem.tests, problem.dependencies) // id, interface, impls, tests, dependencies
        }
    }
}
```

#### Test Generation with EvoSuite

````groovy
action(name: 'evoSuite', type: 'EvoSuite') {
    searchBudget = 30 // we need this as upper bound for timeouts
    stoppingCondition = "MaxTime"
    //criterion = "LINE:BRANCH:EXCEPTION:WEAKMUTATION:OUTPUT:METHOD:METHODNOEXCEPTION:CBRANCH"

    dependsOn 'generateTestsLlama'
    include '*'
    profile('java11Profile')
}
````

Note: EvoSuite does not work with Java > 11.

#### Random Test Generation

```groovy
action(name: 'random', type: 'RandomTestGen') { // add more tests
    noOfTests = 5 // create 5 additional random tests
    shuffleSequence = false

    dependsOn 'typeAware'
    include '*'
}
```

#### Type-Aware Test Mutation

```groovy
action(name: 'typeAware', type: 'TypeAwareMutatorTestGen') { // add more tests
    noOfTests = 1 // create one mutation per existing test

    dependsOn 'generateCodeDeepSeek'
    include '*'
}
```

#### Generate Tests with LLMs

```groovy
action(name: 'generateTestsLlama', type: 'GenerateTestsOllama') {
// pipeline specific
dependsOn 'random'
include '*'
profile('java17Profile')

// action configuration block 
ollamaBaseUrl = "http://bagdana.informatik.uni-mannheim.de:11434"
model = "llama3.1:latest" // LLM
samples = 10 // how many to sample
  
prompt { stimulusMatrix ->
    def prompt = [:] // create prompt model
    prompt.promptContent = """generate a junit test class to test the functionality of the following interface specification: ```${stimulusMatrix.lql}```. Assume that the specification is encapsulated in a class that uses the same naming as in the interface specification. Only output the JUnit test class and nothing else."""
    prompt.id = "lql_prompt"
    return [prompt] // list of prompts is expected
}  
}
```

##### Based on Implementation

```groovy
prompt { stimulusMatrix ->
    List prompts = stimulusMatrix.implementations.collect { impl ->
        def prompt = [:] // create prompt model
        prompt.promptContent = """generate a junit test class to test the functionality of the following java class `${impl.code.name}` : ```${impl.code.content}```. Initialize the class and call its methods. Only output the JUnit test class and nothing else."""
        prompt.id = "lql_prompt"
        return prompt
    }

    return prompts
}  
```

## Arena Test Driver

### Arena: Stimulus Matrices are NOT partitioned by Implementations.

Strategy: full stimulus matrix is processed by one machine.

```groovy
action(name: 'filter', type: 'Arena') { // run all collected stimulus sheets in arena
    maxAdaptations = 1 // how many adaptations to try
    //features = ["cc", "mutation"]
    
    // FIXME run how often? runs = X (then generate different arena ids?)

    dependsOn 'evoSuite'
    include '*'
    profile('java17Profile')
}
```

#### Mutation Testing

Uses Pitest internally.

```groovy
    features = ["mutation"]
```

#### Code Coverage

Uses JaCoCo internally.

```groovy
    features = ["mutation"]
```

### Arena: Stimulus Matrices are Partitioned by Implementations

Strategy: a stimulus matrix is further partitioned into multiple sub-stimulus matrices by partitioning implementations.

Assumes that all tests are available in distributed mode.

```groovy
action(name: 'filter', type: 'ArenaPartitioning') { // run all collected stimulus sheets in arena
    maxAdaptations = 1 // how many adaptations to try
    //features = ["cc", "mutation"]
    
    // FIXME run how often? runs = X (then generate different arena ids?)

    dependsOn 'evoSuite'
    include '*'
    profile('java17Profile')
}
```

### Code Clone Detection

```groovy
action(name: 'filter', type: 'Nicad6') {
    collapseClones = true // drop clones

    dependsOn 'generateCodeLlama'
    include '*'
    profile('nicad:6.2')
}
```

## Data Analysis

### Loading Raw SRM Data

Export raw SRM data from LASSO's Ignite-based database to Parquet.

```
http://{HOST:PORT}/publicapi/v1/lasso/analytics/raw/srm/{SCRIPTID}_all.parquet
```

where _HOST_ and _PORT_ is your service endpoint and _SCRIPTID_ the execution ID of your LSL pipeline.

### Python

https://pandas.pydata.org/docs/reference/api/pandas.pivot_table.html#pandas.pivot_table

```python
# install fastparquet to read parquet files
# for jupyter uncomment the following
# %pip install fastparquet

import pandas as pd

srm_raw = pd.read_parquet('http://lassohp1.informatik.uni-mannheim.de:10222/publicapi/v1/lasso/analytics/raw/srm/dc236539-72a0-48f5-9670-bc7327485778_all.parquet')

# concat implementation id
srm_raw['IMPLEMENTATIONID'] = srm_raw['SYSTEMID'] + '_' + srm_raw['VARIANTID'] + '_' + srm_raw['ADAPTERID']

# filter by output, abstraction and ignore create statements
srm_filtered = srm_raw[(srm_raw['TYPE'] == 'value') & (srm_raw['ABSTRACTIONID'] == 'HumanEval_133_sum_squares') & srm_raw['Y'] > 0]

# SRM for "outputs"
pd.pivot_table(srm_filtered, columns=['IMPLEMENTATIONID'], index=['SHEETID', 'X', 'Y'], values='VALUE', aggfunc='first').reset_index().sort_values(by=['SHEETID', 'X', 'Y'])
```

### SQL (duckDB)

https://duckdb.org/docs/data/parquet/overview.html

```sql
-- first load the raw srm data and then create the output SRM
PIVOT (SELECT SHEETID, X, Y, CONCAT(SYSTEMID,'_',VARIANTID,'_',ADAPTERID) as SYSTEMID, value FROM read_parquet('http://lassohp1.informatik.uni-mannheim.de:10222/publicapi/v1/lasso/analytics/raw/srm/dc236539-72a0-48f5-9670-bc7327485778_all.parquet') where type = 'value' and abstractionid = 'HumanEval_133_sum_squares' and Y > 0) ON SYSTEMID USING first(VALUE) ORDER BY SHEETID, X, Y
```

### R

```
todo
```