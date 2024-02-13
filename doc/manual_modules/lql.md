# LQL (LASSO Query Language) Manual

## Applications

LQL is used to define the interface of a functional abstraction (abstract system) or concrete system.

In _LASSO_ LQL is used to -

* for interface-driven code search (IDCS),
* to define the interface used by sequence sheets (Sequence Sheet Notation - SSN).

## How it works

### IDCS

In IDCS, LQL is translated into a Solr/Lucene query and optional filter queries to retrieve a set of textual candidates that match (are similar to) the given interface specification.

### Sequence Sheet Notation (SSN)

Used to identify SUT.

## ANTLR4 Grammar

The grammar for LQL is written using antlr4.

* Location: [LQL.g4](..%2Flql%2Fsrc%2Fmain%2Fantlr4%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Flql%2FLQL.g4)
* The Java stub is generated as part of Maven's build cycle (e.g., _mvn package_), see antlr4 Maven plugin.

## JUnit Tests (Demonstrations)

The existing JUnit tests demonstrate all features present in LQL -

* Location: [LQLParserTest.java](..%2Flql%2Fsrc%2Ftest%2Fjava%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Flql%2FLQLParserTest.java)

## Language

Typical Format 

```text
InterfaceName {
    constructor(fullyQualifiedInputTypes*)
    methodName(fullyQualifiedInputTypes*)->fullyQualifiedOutputTypes*
} filters*
```

where `InterfaceName` usually denotes the name of the functional abstraction at hand, `constructor` an optional constructor (initializer) and `methodName` zero or more method signatures separated with newline including an optional list of fully-qualified input parameter types as well as output parameter types separated by comma.

Stack Example with two filters that represent a negative list in IDCS (fully optional).

```text
Stack {
    push(java.lang.Object)->java.lang.Object
    pop()->java.lang.Object
    peek()->java.lang.Object
    size()->int
}
!name_fq:Queue !name_fq:Deque
```

## Placeholders

In case the interface name is unimportant or missing (e.g., single methods), the placeholder `$` (in fact any word starting with it) can be used to denote any name -

```text
$ {
    encode(byte[])->java.lang.String
}
```

## Filters

Filters are fully optional. Note that these can also be set in LSL using the _filter_ command as part of the selection action.
