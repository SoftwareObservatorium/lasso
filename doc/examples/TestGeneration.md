# Test Generation

## Obtain new Tests using `EvoSuiteGenerateClass`

This action relies on the external tool [EvoSuite](https://github.com/EvoSuite/evosuite). EvoSuite generates JUnit test suites for Java class automatically. In the following example, we use EvoSuite to generate stimulus sheets which are then executed in the arena to obtain an SRM of actuation sheets.

This requires the presence of an artifact repository for deployment of LASSO support libraries (see [nexus.md](..%2Fnexus.md)).

```groovy
dataSource 'lasso_quickstart'

// interface in LQL notation
def interfaceSpec = """Base64{encodeBase64(byte[])->byte[]}"""
study(name: 'Base64encode-TestGen') {

    profile('java11Profile') { 
        scope('class') { type = 'class' }
        environment('java11') { // EvoSuite does not support recent Java versions
            image = 'maven:3.6.3-openjdk-11'
        }
    }

    /* select one class candidate using interface-driven code search */
    action(name: 'select', type: 'Select') {
        abstraction('Base64') {
            queryForClasses interfaceSpec, 'class-simple'
            rows = 1
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
        }
    }

    /** EvoSuite for unit test generation */
    action(name:'evosuite',type:'EvosuiteGenerateClass') {
        searchBudget = 30

        dependsOn 'select'
        includeAbstractions 'Base64'
        profile('java11Profile')
    }

    /* obtain actuation sheets */
    action(name: 'arena', type: 'ArenaExecute') { // filter by tests
        containerTimeout = 10 * 60 * 1000L // 10 minutes
        exportCsv = true

        dependsOn 'evosuite'
        includeAbstractions 'Base64'
        profile('java11Profile')
    }

    /* rank candidates based on functional correctness */
    action(name:'rank', type:'Rank') {
        // sort by functional similarity (passing tests/total tests) descending
        criteria = ['SelectReport.score:MAX:1'] // more criteria possible

        dependsOn 'arena'
        includeAbstractions '*'
    }
}
```

## Obtain new Tests using `RandomTestGen` (see [RandomTestGen.java](..%2F..%2Fengine%2Fsrc%2Fmain%2Fjava%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Fengine%2Faction%2Ftest%2Fgenerator%2Frandom%2FRandomTestGen.java))

Randomly samples test inputs for a given interface specification.

```groovy
    action(name: 'random', type: 'RandomTestGen') {
        noOfTests = 10
        
        dependsOn 'select'
        includeAbstractions '*'
    }
```

## Obtain new Tests using `TypeAwareMutatorTestGen` (see [TypeAwareMutatorTestGen.java](..%2F..%2Fengine%2Fsrc%2Fmain%2Fjava%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Fengine%2Faction%2Ftest%2Fgenerator%2Ftypeaware%2FTypeAwareMutatorTestGen.java))

Test generation based on a type-aware mutator for test inputs  for a given interface specification (assumes a seed set of tests to mutate).

```groovy
    action(name: 'typeAware', type: 'TypeAwareMutatorTestGen') {
        noOfTests = 1
        
        dependsOn 'select'
        includeAbstractions '*'
    }
```

## Obtain new Tests using `GAITestGen` (see [GAITestGen.java](..%2F..%2Fengine%2Fsrc%2Fmain%2Fjava%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Fengine%2Faction%2Ftest%2Fgenerator%2Fgai%2FGAITestGen.java))

Generates tests with generative AI (GAI) for a given interface specification based on OpenAI Restful API endpoints (optionally, seed tests are used to provide example values to the generative AI).

Alternatives
* OpenAI models https://platform.openai.com/docs/api-reference
* llama.cpp with free (open) models https://github.com/ggerganov/llama.cpp/tree/master/examples/server

For a recent list of possible models, see https://evalplus.github.io/leaderboard.html

```groovy
    action(name: 'gai', type: 'GAITestGen') {
    apiUrl = "http://xxx:8080/v1/chat/completions"
    apiKey = "xxx"
    maxNoOfTests = 100
    noOfPrompts = 1
    // (optional) manual seed tests in terms of sequence sheets
    sequences = [
            'testEncode': sheet(mut:'Problem') {
                row  '',    'create', '?mut'
                row 6l,  'greatestCommonDivisor', 'A1', 54l, 24l
            }
    ]

    dependsOn 'select'
    includeAbstractions '*'
}
```

### APIs like OpenAI's Completions Endpoint

see [gai_models.md](gai_models.md)