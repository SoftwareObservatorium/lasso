# Examples for Action `GitImport`

## Ingest a git repository and analyze its classes

This requires the presence of an artifact repository for deployment (see [nexus.md](..%2Fnexus.md)). 

```groovy
dataSource 'lasso_quickstart'
// interface in LQL notation
def interfaceSpec = """Base64{encode(byte[])->byte[]}"""
/** Define a new study */
study(name: 'GitImporterDemo') {

    /** defines profile (compiler etc.) */
    profile('java17Profile') {
        scope('class') { type = 'class' } // measurement scope
        environment('java17') { // execution environment
            image = 'maven:3.6.3-openjdk-17' // (docker) image template
        }
    }
    
    /** Import project(s) from Git repositories */
    action(name: 'import', type: 'GitImport') {
        repositories = [
                'codec': 'https://github.com/apache/commons-codec.git',
        ]
        deploy = true // should be set to true
        
        profile('java17Profile') // Maven image has git onboard
    }
    
    /** Let's do an interface-driven code search */
    action(name: 'select', type: 'Select') {
        abstraction('Base64') {
            queryForClasses interfaceSpec, 'class-simple'
            rows = 10
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()

            // make sure to select only from the imported repos above
            filter 'executionId:"' + lasso.executionId + '"' // current execution id
            filter 'action:"import"' // unique action (see above)
        }
    }

    /** Print some debug information (see workspace/execution_log.txt) */
    action(name: 'debug', type: 'Debug') {
        dependsOn 'select'
        includeAbstractions 'Base64'
        
        execute {
            int size = abstractions['Base64'].systems.size()
            log("no of classes found. ${size}")
        }
    }
}
```