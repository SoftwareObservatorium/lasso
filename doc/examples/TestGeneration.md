# Examples for Unit Test Generation

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
