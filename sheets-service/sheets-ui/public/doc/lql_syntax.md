## LQL - LASSO Query Language Syntax

Each specified method in separate line (separated by new line). The notation uses a Python-like syntax for typing information.

#### Notation

```java
ClassName {
    methodName1(paramType1, paramType2)->returnType
    methodName2...
}
```

##### Methods

```java
methodName1(paramType1, paramType2)->returnType
```

##### Constructors (Initializers)

Initializers are used to create new instances (cf. constructors)

```java
ClassName(paramType1, paramType2)
```

Note that the return type of initializers is implicit, since they always return the type of `ClassName` (as for constructors in Java/Python).

How instances are actually obtained is an implementation detail (could be through constructors, singletons, factories, no initialization (i.e., static method calls) etc.).

#### Example

```java
Stack {
    push(java.lang.String)->java.lang.String
    size()->int
}
```