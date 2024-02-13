# Data Sources

This module contains data source related logic, including -

* interface to single underlying corpus (cf. crawler, analyzer)
* LQL to Solr/Lucene queries translation

## LQL to Solr/Lucene queries translation


### NLP Features

Mainly used for query expansion.

#### WordNet

Synonyms and Antonyms

```xml
<!-- main library dependency -->
<dependency>
    <groupId>net.sf.extjwnl</groupId>
    <artifactId>extjwnl</artifactId>
    <version>2.0.5</version>
</dependency>
<!-- Princeton WordNet 3.1 data dependency -->
<dependency>
    <groupId>net.sf.extjwnl</groupId>
    <artifactId>extjwnl-data-wn31</artifactId>
    <version>1.2</version>
</dependency>
```

#### Code2Vec (Word2Vec) - Word Embeddings

Co-occuring method names.

See [code2vec](https://github.com/tech-srl/code2vec/)

* [method names](https://s3.amazonaws.com/code2vec/model/target_vecs.tar.gz)
* [application.properties](..%2Fservice%2Fsrc%2Fmain%2Fresources%2Fapplication.properties)

Must be set via -D

```text
models.embedding.code2vec = /home/swtlasso/models/target_vecs.txt
```

### LQL

* _$_ used for name placeholders (class and method)
* _?_ used for type placeholders (type doesn't matter, e.g., it doesn't matter what is returned, can be void as well)
* 