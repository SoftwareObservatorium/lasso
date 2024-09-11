## Sequence Sheet Notation - Syntax

Functional abstractions

* either conceptual (virtual implementation), or,
* an executable description from which the interface is derived (i.e., existing class)

### Types of Sheets

* Stimulus Response Sheets - written against a functional abstraction
* Actuation Sheets - contains actuation based on the level of specification of a functional abstraction
* Adapted Actuation Sheets - actual actuation sheet based on the adapted implementation (white box view on actual interface specification and actuations resolved for implementation)

### Specification - Cell Value Types

| Cell Type   |      Description      |  Example |
|----------|:-------------:|------:|
| Object value |  Primitive values as well as strings and arrays (lists) | `1` for integer or `"hello"` for strings |
| Object reference |    a reference to an object using the spreadsheet notation   |   `A1` |
| Command invocation | invoke a special command | `$create` |
| Method invocation | invoke a method on an instance (either specified by LQL interface or a classic method of language classes) | `push` or `toString` |
| Code expression | evaluate a language-specific code expression | `Arrays.toString(new char[]{'a', 'b'})` |

### Special Commands

| Command |      Description      |  Example |
|---------|:-------------:|------:|
| `create`  |  create instance | $1600 |
| `$create` (or `create`) |  create instance   |  Class under test: `my.Cut` or JDK-specific `java.util.ArrayList` |
| `$eval`   | Evaluate a code expression (e.g., to create test data) | `Arrays.toString(new char[]{'a', 'b'})` |

### Results - Cell Value Types

Essentially, object references get resolved and a special notation is used to serialize them.

| Cell Type   |      Description      |  Example |
|----------|:-------------:|------:|
| Object value |  Primitive values as well as strings and arrays (lists) | `1` for integer or `"hello"` for strings |
| Object reference |    a reference to an object: type@name@ID   |   `$CUT@Stack@0` |
| Command invocation | invoke a special command | `$create` |
| Method invocation | invoke a method on an instance (either specified by LQL interface or a classic method of language classes) | `push` or `toString` |
| Code expression | evaluate a language-specific code expression | `Arrays.toString(new char[]{'a', 'b'})` |