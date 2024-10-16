# SRMs/SRHs - Software Analytics (Offline)  

LSL pipeline script executions result in one or more SRMs that are stored in LASSO's distributed database. The collection of SRMs effectively results in an SRM data warehouse that we refer to as SRH (stimulus response hypercube).

SRHs including SRMs are accessible in external data analytic tools in which observational records can be analyzed.

## Jupyterlite Integration in LASSO (experimental)

As an experimental feature, SRMs obtained as part of LSL pipeline script executions can be analyzed using _jupyterlite_. Note that jupyterlite runs entirely in the browser (including python, see https://jupyterlite.readthedocs.io/en/stable/), hence it is limited with respect to the size of data sets it can process compared to classic jupyterlab deployments that come with a python backend.

![quickstart_jupyterlite_webui.png](img%2Fquickstart_jupyterlite_webui.png)
![quickstart_jupyterlablite.png](img%2Fquickstart_jupyterlablite.png)

### Export to Parquet

SRM-related data can be exported to the popular Parquet format (supported by all popular analytics tools).

see RESTful endpoint in [AnalyticsController.java](..%2Fservice%2Fsrc%2Fmain%2Fjava%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Fservice%2Fcontroller%2Fanalytics%2FAnalyticsController.java)

### Export to Jupyter Notebooks

As an experimental feature, basic Jupyter notebooks can be generated automatically for individual LSL script executions. This is currently used to open SRM data in LASSO's web application using jupyterlite which allows analytics using Pandas that run entirely in the browser (limited with respect to dataset size).

see RESTful endpoint in [AnalyticsController.java](..%2Fservice%2Fsrc%2Fmain%2Fjava%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Fservice%2Fcontroller%2Fanalytics%2FAnalyticsController.java)


## Python (pandas)

Examples to manipulate SRMs with Python pandas (https://pandas.pydata.org/) are available as interactive notebooks in _jupyterlab_ playground on our project website

* https://softwareobservatorium.github.io/jupyterlab/lab/index.html

![quickstart_jupyterlab.png](img%2Fquickstart_jupyterlab.png)

## Python (pyignite)

Since LASSO uses Apache Ignite, an official Python module `pyignite` (https://ignite.apache.org/docs/latest/thin-clients/python-thin-client) is available to access SRHs in Python based on Ignite's concept of thin clients. The client can be used to manipulate SRMs/SRHs using popular data frame libraries including `pandas` (https://pandas.pydata.org/). This also offers the possibility to interactively explore SRMs as part of Jupyter notebooks.

```python
from pyignite import Client
import pandas as pd

# LASSO manager node (IP, DNS)
lasso_host = '127.0.0.1'
lasso_port = 10800

# Ignite thin client
client = Client()
client.connect(lasso_host, lasso_port)

# LSL script execution ID
my_script_id = 'd884938e-8c30-4549-8185-26ac3f95a3b2'

# use SQL (see https://apache-ignite-binary-protocol-client.readthedocs.io/en/latest/index.html)
cursor = client.sql("SELECT * FROM srm.CELLVALUE where where executionId = ? and type != 'seq' and type != 'exseq'", query_args= [my_script_id], include_field_names=True)
column_headers = next(cursor)
rows = list(cursor)

# SQL cursor to pandas DataFrame
df = pd.DataFrame(rows, columns=column_headers)
# print tabulated
print(df.to_markdown())
# manipulate data ...
print(df['EXECUTIONID'].unique())
```


## R Language (RJDBC)

To manipulate SRMs in R, we can use the `RJDBC` package (https://cran.r-project.org/web/packages/) that uses Ignite's official JDBC driver (https://ignite.apache.org/docs/2.14.0/SQL/JDBC/jdbc-driver.html) under the hood.

For this to work, you need download Ignite's binary release (https://ignite.apache.org/) and unzip it. 

```r
library(RJDBC)

# modify path
drv <- JDBC("org.apache.ignite.IgniteJdbcThinDriver",
            "/your/path/to/apache-ignite-2.14.0-bin/libs/ignite-core-2.14.0.jar",
            identifier.quote="`")

# example function to retrieve SRM-related data for a certain LSL script execution id 
getSrm <- function(jdbcUrl, executionId) {
  connectUrl <- paste("jdbc:ignite:thin://", jdbcUrl, "/", sep = "")
  conn <- dbConnect(drv, connectUrl)
  query <- paste("SELECT * from ", "srm.CellValue", " where executionId = '", executionId, "' and type != 'seq' and type != 'exseq'", sep = "")
  report <- dbGetQuery(conn, query)
  dbDisconnect(conn)
  
  report
}

# utility function to join multiple SRMs
joinSrm <- function(jdbcUrl, executionIds) {
  results <- NULL
  for(executionId in executionIds) {
    r <- getSrm(jdbcUrl, executionId[1])
    
    if(is.null(results)) {
      results <- r
    } else {
      results <- rbind(results, r)
    }
  }
  
  results
}

# points to your LASSO manager node (IP or DNS)
jdbcUrl <- "127.0.0.1"

# LSL script execution ids (see LASSO dashboard)
script_ids <- list(
  c("d884938e-8c30-4549-8185-26ac3f95a3b2", "MyExecutedPipeline")
  # more ...
)

# fetch srm
srm <- joinSrm(jdbcUrl, script_ids)

# now do some manipulations using tidyverse for example (e.g., dplyr)
library(dplyr)
...
```

## DuckDB (Java JDBC API) using Apache Arrow

It is possible to use DuckDB in Java via JDBC (https://duckdb.org/docs/api/java) by using Apache's Ignite JDBC over Apache Arrow (https://arrow.apache.org/). 

Code examples can be found in [ArrowOlap.java](..%2Fengine%2Fsrc%2Fmain%2Fjava%2Fde%2Funi_mannheim%2Fswt%2Flasso%2Fsrm%2Folap%2FArrowOlap.java)

Currently, DuckDB is used in a similar way to export SRM-related data to the popular Parquet format (https://parquet.apache.org/) which can by imported by many popular analytics tools.

Note that a Python API exists as well (see https://duckdb.org/docs/api/python/overview).