# Analyzer Module

Indexation of (Maven) artifacts.

Assumes both binary and source code artifacts -

* .jar
* -tests.jar

Also supports test artifacts (same way as production artifacts).

## Usage

### Batch Processing

Mass processing with `indexer` module.

Used for mass indexation of artifacts.

## Single Project Processing

Single Maven project with `index_maven_plugin`.

Used for ad hoc indexing of artifacts as part of LSL pipelines.