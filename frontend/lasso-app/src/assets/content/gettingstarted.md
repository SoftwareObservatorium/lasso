## Getting Started

LASSO Web Frontend

### Editor

Allows LSL scripts to be written and to be executed.

### My Scripts

Collection of previously executed scripts.

* `Execution ID` - Unique ID of LSL script execution.
* `Study Name` - Name of the LSL study (as provided in the study block of the LSL script)
* `Status` - Execution status (successful or failed)
* `Start` - Start of script execution (execution depends job scheduling policy)
* `End` - End of script execution
* `Workspace` - View/download workspace of all files related to given script execution. The manager (named `master`) node hosts all CSV-related files whereas the `worker` nodes (identified by their UUID) host action-related files (e.g., builds, generated tests etc.)
* `Script` - Opens the script in the LSL editor
* `DB` - Allows querying script-related SRM data as well as complex reports generated by actions (selectable via dropdown list)
* `Results` - Provides results view on the systems managed by the script's last known action (often used as part of LASSO Search)

### Actions

Presents an overview/documentation of known LASSO Actions and their properties.

### Search

Provides classic code search engine capabilities as part of LASSO Search.

### Dashboard

#### Cluster Health

Allows monitoring of all nodes participating in the Apache Ignite Cluster.

#### Master Log

Allows viewing the logs of the manager node.