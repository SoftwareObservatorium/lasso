# Quickstart guide to set up LASSO in standalone mode

This guide provides the basic steps to set up the LASSO platform in standalone mode on a single, local machine.

We use the running example of a test-driven code search for Base64 implementations by ingesting the popular library _Apache Commons Codec_ into LASSO's executable corpus.

Important: Possible security risks are not taken into consideration, so do not expose your instances.

## Requirements / Assumptions

* Linux (tested using Ubuntu 22.04 LTS) - MacOS and Windows is untested
* a working docker installation, preferably running as a non-root user (e.g., https://docs.docker.com/engine/install/ubuntu/)
* Java JDK >= 11 (any free JDK distribution should work)
* the frontend modules (webapps using angular) are built with nodejs/npm (retrieved automatically using a Maven plugin (https://github.com/eirslett/frontend-maven-plugin/)
* LASSO uses Apache Ignite (https://ignite.apache.org/), so several ports are opened automatically to enable cluster (grid) communication

## Building LASSO

The project is managed using Maven by relying on _Maven Wrapper_ (https://maven.apache.org/wrapper/) for building all required modules (your local Maven may work as well).

The following command needs to be executed in the root directory of the repository:

```bash
./mvnw -DskipTests \
  -Dfrontend.build=embedded \ # profile for localhost:10222
  clean install
```

The chosen profile (i.e., `embedded`) for the webapps assume that LASSO's RESTful webservice will be running on `localhost:10222`.

For each module, the builds are available in the corresponding `target/*.jar` folders.

## Set up an executable corpus

In the next step, we set up a new executable corpus which consists of two components -

1. code search index using Solr/Lucene,
2. code repository using Sonatype Nexus OSS.

### Code Search: Setting up a Solr index

Why? The code search index is populated by LASSO to enable interface-driven code searches.

see detailed instructions in [solr.md](solr.md).

### Code Repository: Setting up a software artifact repository

Why? The code repository stores executable artifacts and acts as a proxy for existing artifacts (including Maven Central by default).

see detailed instructions in [nexus.md](nexus.md).

## Start to ingest software artifacts

### Fetch a Maven Artifact (crawler module)

Why? We want to demonstrate the ingestion (import) of new artifacts into LASSO's executable corpus.

This uses functionality provided by the [crawler](../crawler) module.

In this example, we aim to ingest `Apache Commons Codec 1.15` (sources and bytecode) and make it available in the executable corpus.

```bash
# create working directory (where artifacts are stored)
mkdir lasso_crawler

# run crawler to download commons-codec
java -Dartifacts=commons-codec:commons-codec:1.15:sources \
    -Dindexer.work.path=lasso_crawler \
    -Dbatch.maven.repo.url=http://localhost:8081/repository/maven-public/ \
    -Dlasso.indexer.worker.threads=1 \
    -jar crawler/target/crawler-1.0.0-SNAPSHOT.jar
```

where

* `artifacts` takes a '|' separated list of Maven coordinates (format: `groupId:artifactId:version:classifier`)
* `indexer.work.path=lasso_crawler` points to your working directory
* `batch.maven.repo.url` points to your nexus repository
* `lasso.indexer.worker.threads` sets the number of worker threads for crawling artifacts

Note that the `crawler` module can also be used to index entire Maven-compatible repositories including Maven Central based on Nexus indices (https://maven.apache.org/repository/central-index.html).

It is also possible to ingest _git_ repositories (see examples in [pipelines.md](pipelines.md)).

### Analyze and index software artifacts

Why? We aim to analyze (code analytics) and index the previously downloaded artifact(s) to enable code searches.

This uses functionality provided by the [analyzer](../analyzer) module.

The following command first conducts static code analysis and then populates the results in the Solr index `lasso_quickstart` to enable code search

```bash
# run analyzer (points to directory of crawler above)
java -Xms2g -Xmx2g \
    -Dindexer.work.path=lasso_crawler/ \
    -Dlasso.indexer.worker.threads=4 \
    -Dbatch.job.writer.threads=-1 \
    -Dbatch.job.commit.interval=1 \
    -Dbatch.solr.url=http://localhost:8983/solr \
    -Dbatch.solr.core.candidates=lasso_quickstart \
    -jar analyzer/target/analyzer-1.0.0-SNAPSHOT-exec.jar
```

where

* `indexer.work.path=lasso_crawler/` points to your crawler working directory
* `lasso.indexer.worker.threads` sets the number of worker threads for generating Solr documents
* `batch.job.writer.threads sets` the number of writer threads for Solr
* `batch.job.commit.interval sets` the commit interval for committing Solr documents (batching)
* `batch.solr.url=http://localhost:8983/solr` sets the Solr url
* `batch.solr.core.candidates=lasso_quickstart` sets the Solr core (i.e., code search index)

Now, you can now open your web browser and go to http://localhost:8983/solr/#/lasso_quickstart/query to see the results.

When you hit _Execute Query_, hundreds of documents should appear that describe the code that has been indexed. There are two types of documents present: class- and method documents.

You can try simple keyword queries with Solr's query syntax such as the query (i.e., q) `name_fq:"Base64"` to retrieve all classes similar to `Base64`. You can add a filter query (i.e., fq), by only returning all method (documents) of the classes found (i.e., `doctype_s:"method"`),

See https://solr.apache.org/guide/solr/latest/query-guide/query-syntax-and-parsers.html for Solr's query syntax.

A description of LASSO's index schema is in [index.md](index.md).

## Starting LASSO (standalone mode)

Next, we set up the LASSO platform to run on a single machine. In this case, the platform runs in `embedded` mode, so both the manager node as well as one worker node are running on the same machine.

### Option 1) Docker container

To get started, running LASSO's service in docker is the simplest way to set it up.

see detailed instructions in [docker.md](docker.md).

The _Dockerfile_ is located in [Dockerfile](..%2Fdocker%2Fservice_embedded%2FDockerfile)

### Option 2) Local Java

LASSO's service can also be run as a local Java application (Spring Boot).

The following commands first set up

* the configuration (executable corpus and users), and
* the working directory in which pipeline scripts executions and traces are stored.

```bash
# create LASSO work directory
mkdir lasso_work
# create config
mkdir lasso_config
cp lasso_config/users.json lasso_config/
cp lasso_config/corpus.json lasso_config/

# copy over arena jar
mkdir -p lasso_work/repository/support/
cp arena/target/arena-1.0.0-SNAPSHOT-exec.jar lasso_work/repository/support/arena-1.0.0-SNAPSHOT.jar

# start LASSO in embedded mode (--add-opens arguments are required for Java > 11)
java --add-opens java.base/java.nio=ALL-UNNAMED\
    --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED\
    --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED\
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED\
    --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED\
    --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED\
    --add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED\
    --add-opens=java.base/java.io=ALL-UNNAMED\
    --add-opens=java.base/java.nio=ALL-UNNAMED\
    --add-opens=java.base/java.util=ALL-UNNAMED\
    --add-opens=java.base/java.util.concurrent=ALL-UNNAMED\
    --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED\
    --add-opens=java.base/java.lang=ALL-UNNAMED\
    
    -server -ea -Xms2G -Xmx4G\
    -Dserver.port=10222 -Djava.net.preferIPv4Stack=true -Dcluster.nodeId=lasso-quickstart -Dcluster.embedded=true\
    -Dthirdparty.docker.uid=$(id -u) -Dthirdparty.docker.gid=$(id -g)\
    -Dlasso.workspace.root="$PWD/lasso_work/"\
    -Dusers="file:$PWD/lasso_config/users.json"\
    -Dcorpus="file:$PWD/lasso_config/corpus.json"\
    -jar service/target/service-1.0.0-SNAPSHOT.jar
```

Example configuration files (JSON) for this quickstart guide of the corpus and the users are located in [lasso_config](lasso_config).

Despite the verbose arguments needed to run the platform on Java >= 11 (i.e., add-opens), here is a quick description of the arguments

```
# allocate sufficient memory for LASSO's Ignite cluster (depends on specific workload)
-server -ea -Xms2G -Xmx4G

# configures the Ignite cluster and tells the engine to run in embedded mode 
-Dserver.port=10222 -Djava.net.preferIPv4Stack=true -Dcluster.nodeId=lasso-quickstart -Dcluster.embedded=true

# some docker images require the correct user/group ids of the host to avoid access problems   
-Dthirdparty.docker.uid=$(id -u) -Dthirdparty.docker.gid=$(id -g)

# set working directory for LASSO in which executions/traces are stored
-Dlasso.workspace.root="$PWD/lasso_work/"

# sets the users required to access the webapps / RESTful API
-Dusers="file:$PWD/lasso_config/users.json"

# sets the corpus configuration
-Dcorpus="file:$PWD/lasso_config/corpus.json"
```

### Submit an LSL Script Pipeline using LASSO's Dashboard (webapp)

The platform comes with a dashboard to manage, monitor and view results of pipeline scripts and their execution. In addition, it allows users to search code etc.

At time of writing, there are two webapps available

* the new angular GUI based on material design is available at http://localhost:10222/webui/
* the old angular GUI based on bootstrap is available at http://localhost:10222/lasso/

To submit a new script, follow these steps

1. Login by picking a user(s) from [users.json](lasso%2Fusers.json)
2. Submit a new LSL script pipeline.

To exemplify, the following LSL script pipeline realizes a test-driven code search using the LASSO platform for classes (methods) that realize Base64 encoding. Copy and paste the following script into the LSL script editor in the dashboard and submit it for execution.

```groovy
dataSource 'lasso_quickstart'

def totalRows = 10
def noOfAdapters = 100
// interface in LQL notation
def interfaceSpec = """Base64{encode(byte[])->byte[]}"""
study(name: 'Base64encode') {
    /* select class candidates using interface-driven code search */
    action(name: 'select', type: 'Select') {
        abstraction('Base64') {
            queryForClasses interfaceSpec, 'class-simple'
            rows = totalRows
            excludeClassesByKeywords(['private', 'abstract'])
            excludeTestClasses()
            excludeInternalPkgs()
        }
    }
    /* filter candidates by two tests (test-driven code filtering) */
    action(name: 'filter', type: 'ArenaExecute') { // filter by tests
        containerTimeout = 10 * 60 * 1000L // 10 minutes
        specification = interfaceSpec
        sequences = [
                // parameterised sheet (SSN) with default input parameter values
                // expected values are given in first row (oracle)
                'testEncode': sheet(base64:'Base64', p2:"user:pass".getBytes()) {
                    row  '',    'create', '?base64'
                    row 'dXNlcjpwYXNz'.getBytes(),  'encode',   'A1',     '?p2'
                },
                'testEncode_padding': sheet(base64:'Base64', p2:"Hello World".getBytes()) {
                    row  '',    'create', '?base64'
                    row 'SGVsbG8gV29ybGQ='.getBytes(),  'encode',   'A1',     '?p2'
                }
        ]
        features = ['cc'] // enable code coverage measurement (class scope)
        maxAdaptations = noOfAdapters // how many adaptations to try

        dependsOn 'select'
        includeAbstractions 'Base64'
        profile('myTdsProfile') {
            scope('class') { type = 'class' }
            environment('java11') {
                image = 'maven:3.6.3-openjdk-17' // Java 17
            }
        }

        // match implementations (note no candidates are dropped)
        whenAbstractionsReady() {
            def base64 = abstractions['Base64']
            def base64Srm = srm(abstraction: base64)
            // define oracle based on expected responses in sequences
            def expectedBehaviour = toOracle(srm(abstraction: base64).sequences)
            // returns a filtered SRM
            def matchesSrm = srm(abstraction: base64)
                    .systems // select all systems
                    .equalTo(expectedBehaviour) // functionally equivalent
        }
    }
    /* rank candidates based on functional correctness */
    action(name:'rank', type:'Rank') {
        // sort by functional similarity (passing tests/total tests) descending
        criteria = ['FunctionalSimilarityReport.score:MAX:1'] // more criteria possible

        dependsOn 'filter'
        includeAbstractions '*'
    }
}
```

The pipeline defines a study block in which three actions are executed

1. _select_ action: The first action selects candidates textually using an interface-driven code search from the index we have created earlier in this guide (i.e., `lasso_quickstart`), see [datasources.json](lasso%2Fdatasources.json)
2. _filter_ action: The second action defines two tests (including expected values) using the sequence sheet notation, and runs them on the textually selected candidates. Only those candidates are returned by the action which match the expected behaviour defined in the first column of the test sequences.
3. _rank_ action: Finally, the third action sorts all candidates in descending order based on their passing rate (passed tests/total tests).

Once the execution has ended, the overview site of all executed scripts in the dashboard offers various ways to obtain the results (e.g., viewing the results in a classic search results view, `Results`, or analyzing the data stored in LASSO's database etc.).

![quickstart_results.png](img%2Fquickstart_results.png)

### Remarks

#### Scalability / Distributed Mode

Next to vertical scaling of code analysis, the LASSO platform also scales vertically, hence is designed to run on more than one node. See [distributed.md](distributed.md) for more information.

## Summary

Here is a quick summary of the three services that were created as part of this guide.

### Executable corpus

* The code search index (Solr) is located here: http://localhost:8983/solr/lasso_quickstart (dashboard is available here: http://localhost:8983/solr/#/lasso_quickstart/query)
* The artifact repository (Nexus) is located here:  http://localhost:8081/repository/maven-public/ (dashboard is available here: http://localhost:8081/)

### LASSO platform

* LASSO instance http://localhost:10222
* New GUI: http://localhost:10222/webui/
* Old GUI: http://localhost:10222/lasso/


## Advanced Topics (Tool integrations etc.)

LASSO is extensible via its Actions API. See [actions.md](actions.md) for more information.