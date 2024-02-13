# Arena

The arena module of the platform. Its core features include -

* the sequence sheet engine for executing tests on systems
* storing systems' responses in SRMs
* structural adaptation

Currently, it is based on randoop's dynamic reflection-based execution engine to execute operations and to gather responses.

## Modules

* `arena-support` - adaptation support and shared classes with other modules
* `arena` - test driver to execute sequence sheets

## Usage

Typically called by LASSO Actions via docker containerization using the command line. This requires distributed job support (the arena uses a built-in client to get the job description).

It acts as a standalone test driver for sequence sheets.

## Artifacts

* artifacts are retrieved on-the-fly based on their Maven coordinates

## Execution Environment (Isolated ClassLoader)

* execution of Java classes/methods is realized in isolated ClassLoaders based on `classworlds`
* sheet execution engine is currently based on randoop's dynamic reflection-based execution engine

### Program Analysis (and Instrumentation)

Program analysis as well as measurement are realized on top of isolated ClassLoader that allow to hook into the classloading mechanism (e.g., to instrument code).

The observation model can be used to record measurements (similar to stimulus/response observations).

Currently, several features can be plugged into isolated ClassLoaders that support custom instrumentation as well as introspection, including -

* JaCoCo code coverage (code instrumentation)
* Pit (mutation analysis)
* (simple) dynamic call graphs (dcg)

### Measurement (scope-based)

scoping model + filtering.

## Tasks

* `Execute` - execute tests on a set of sequence sheets
* `Amplify` - execute tests from one system on other systems

## Parser

* Sequence Sheet Notation
* JUnit tests

## JUnit Runner/Export

* Sequence sheets can be exported to JUnit (based on randoop)
* JUnit tests can be executed and checked for validity

## SRM

CellValue model

* Observation
* Records

## Job Retrieval (via engine using Apache Ignite)

* JobClient as well as SRMClient