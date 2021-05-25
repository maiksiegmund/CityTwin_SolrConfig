# CityTwin_SolrConfig
includes models and configs to find locations in textdocuments

Steps:

- apache [solr](https://solr.apache.org/guide/8_8/solr-tutorial.html) standard installation
- change directory to "solr/bin"
- solr start -m 8g -V
- create core
  - solr create -c "collection_name"
- edit managed-schema
- edit solrconfig.xml
- index data via post tool (include in "solr/bin" directory)
  - post -c "collection_name" "files"
  - example: ./post -c citytwin_managed /media/sf_sharedFolder/festsetzungbegruendung-xvii-50aa.pdf


#### optional reindex by script

#!/bin/sh
collection="citytwin_managed"

#### delete content stop and start
- curl -X POST -H 'Content-Type: application/json' --data-binary '{"delete":{"query":"*:*" }}' http://localhost:8983/solr/$collection/update
- rm /home/debian/solr/solr-8.8.0/server/solr/$collection/data/index/*
- rm /home/debian/solr/solr-8.8.0/server/solr/$collection/data/snapshot_metadata/*
- rm /home/debian/solr/solr-8.8.0/server/solr/$collection/data/tlog/*
- /home/debian/solr/solr-8.8.0/bin/solr stop -V
- /home/debian/solr/solr-8.8.0/bin/solr start -m 8g -V
    

## citytwin_managed 

location finding by stopword and keepword filter
stopword list of german words see file stopwords_de.txt
keepword list of berlin addresses (streets, not completely)

## city_ner
apache [opennlp](https://opennlp.apache.org/docs/1.5.3/manual/opennlp.html#tools.namefind.recognition) Named Entity Recognition

models trained on german text corpus [GermEval 2014](https://sites.google.com/site/germeval2014ner/)

algorithm: 
- maxent_qn
- maxent
- perceptron
- naivebayes

## tools

contains:
- origin data 
- scripts 
- sourcecode 






