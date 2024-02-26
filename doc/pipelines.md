# LASSO Scripting Language

## Pipelines (examples)

### Interface-Driven Code Search

`Select` action

```groovy
dataSource 'lasso_quickstart'

def totalRows = 10
// interface in LQL notation
def interfaceSpec = """Base64{encode(byte[])->byte[]}"""
study(name: 'Base64encode') {
    action(name: 'select', type: 'Select') {
        abstraction('Base64') { // interface-driven code search
            queryForClasses interfaceSpec, 'class-simple'
            rows = totalRows
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
        }
    }
}
```

### Test-Driven Code Search

`ArenaExecute` action.

see [quickstart.md](quickstart.md).

### Automated, Unit Test Generation

`EvoSuiteGenerateClass` action.

see [TestGeneration.md](examples%2FTestGeneration.md).

### Generative AI (ChatGPT)

Assess code returned by GAI using `GenerativeAI` action.

see [GenerativeAI.md](examples%2FGenerativeAI.md).

### Git - ingest and analyze projects managed via git

`GitImport` action

see [GitImport.md](examples%2FGitImport.md).

## Actions

Existing actions are documented in the `webui` module (see http://localhost:4200/actions).

A developer-oriented introduction to the creation of new actions is available in [actions.md](actions.md)