# LASSO Scripting Language

## Pipelines (examples)

### Interface-Driven Code Search

Select

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

ArenaExecute

see [quickstart.md](quickstart.md).

### Generative AI (ChatGPT)

Assess code returned by GAI.

see [GenerativeAI.md](examples%2FGenerativeAI.md).

### Git - ingest and analyze projects managed via git

GitImport

see [GitImport.md](examples%2FGitImport.md).