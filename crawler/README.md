# Changes 08.05.25

Issues with automatic download of index (too large to unzip programmatically?)

## Manual Download of Index

see https://maven.apache.org/repository/central-index.html

see https://repo.maven.apache.org/maven2/.index/

download the Central index: nexus-maven-repository-index.gz
download Maven Indexer CLI and unpack the index to raw Lucene index directory:

```bash
java -jar indexer-cli-5.1.1.jar --unpack nexus-maven-repository-index.gz --destination central-lucene-index --type full
```

Results in same directory as created by crawler component

Run crawler

```bash
wget "http://swtweb.informatik.uni-mannheim.de/nexus/repository/maven-snapshots/de/uni-mannheim/swt/lasso/crawler/1.0.0-SNAPSHOT/crawler-1.0.0-20250508.085922-88.jar" # identify latest snapshot ..
java -Xms60G -Xmx60G -Dindexer.work.path=lasso_crawler -Dbatch.maven.repo.url=https://repo1.maven.org/maven2/ -Dlasso.indexer.worker.threads=8 -Dbatch.maven.index.update=false -Dbatch.maven.latest.head=1 -jar crawler-1.0.0-20250508.085922-88.jar
```