# CellValue Model (SRM)

The _CellValue_ model is used to store SRM-related observational data.

## Underlying Schema that models cell values of an SRM (inspired by EAV)

### Apache Ignite

```sql
CREATE MEMORY TABLE SRM.CELLVALUE(
    -- Apache Ignite key (internal)
    _KEY OTHER INVISIBLE  NOT NULL, 
    -- Apache Ignite value (internal)
    _VAL OTHER INVISIBLE ,
    -- LSL Script execution ID
    EXECUTIONID VARCHAR NOT NULL,
    -- LSL Action ID
    ACTIONID VARCHAR NOT NULL,
    -- Abstraction container ID
    ABSTRACTIONID VARCHAR NOT NULL,
    -- Arena Execution ID
    ARENAID VARCHAR NOT NULL,
    -- Sequence Sheet ID (typically name of the test)
    SHEETID VARCHAR NOT NULL,
    -- ID of the code as it appears in the code index
    SYSTEMID VARCHAR NOT NULL,
    -- A variant of the code depicted by the SYSTEMID above (e.g., mutant code)
    VARIANTID VARCHAR NOT NULL,
    -- ID of a particular adapter for the code depicted by the SYSTEMID above as generated part of the adaptation process
    ADAPTERID VARCHAR NOT NULL,
    -- Sequence coordinate X (i.e., part of a statement, >= 0)
    X INT NOT NULL,
    -- Sequence coordinate Y (i.e., statement, >= 0)
    Y INT NOT NULL,
    -- Observation type (e.g., 'value' for output, 'input_value' for input etc.)
    TYPE VARCHAR,
    -- Observation value (serialized)
    VALUE VARCHAR,
    -- Raw observation value (unserialized)
    RAWVALUE VARCHAR,
    -- Object type of observation value
    VALUETYPE VARCHAR,
    -- Timestamp of observation
    LASTMODIFIED TIMESTAMP,
    -- Execution time (i.e., for observation values)
    EXECUTIONTIME BIGINT
)
```

### Identifying Arena Executions

Combination of

* _EXECUTIONID_
* _ACTIONID_
* _ARENAID_

In other words, an instance of the arena test driver with ID _ARENAID_ is executed as part of an action _ACTIONID_ defined by the executed LSL pipeline script _EXECUTIONID_.  

Frequent values for the _ARENAID_ column include

* `execute*` - for normal test execution
* `jacoco*` - for code measurements
* `pitest*` - for mutation testing

### Identifying Implementations

Combination of

* _SYSTEMID_
* _ADAPTERID_
* _VARIANTID_

A code unit with ID _SYSTEMID_ (as stored in the code index) using adapter _ADAPTERID_. Sometimes, we generate variants of a code unit (e.g., mutated code) which are depicted by _VARIANTID_.

### Identifying Sequence Sheets

Combination of

* _SHEETID_ (test name)
* _X_ (x-coordinate of the cell)
* _Y_ (y-coordinate of the cell; it identifies the statement)

Examples

* `(X=0,Y)` refers to the output value column in a sequence sheet
* `(X=1,Y)` refers to the operation column in a sequence sheet
* `(X>1,Y)` refers to the input value columns
* `(0,0)` refers to the output value by the `CREATE` statement and stores the instance of the class under test as its output

Note that _SHEETID_ is currently represented as a (string) compound key of
* test name
* SYSTEMID

separated by `_`. For data manipulation, the postfix part needs to be removed:

* `testName_SYSTEMID` -> `testName`

### Identifying Observations

Combination of

* _VALUE_
* _TYPE_

Frequent values for the _TYPE_ column (i.e., observational types) include

* `value` - for output observations part of responses
* `input_value` - for input observations part of stimuli
* `op` - operation name
* `seq` - Java sequence of statement
* `exseq` - Test sequence of statements (JUnit like)
* `jacoco_*` - for code measurements