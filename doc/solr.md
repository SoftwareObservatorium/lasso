# Solr & LASSO

This short guide demonstrates how to set up a new code search index as part of LASSO's executable corpus.

Important: Possible security risks are not taken into consideration, so do not expose your instances.

## Quickstart guide (docker)

This guide assumes that a working docker installation (user space level, see https://docs.docker.com/engine/install/linux-postinstall/) is present on the local machine. 

It uses the official docker image to run the latest version of Solr (see https://hub.docker.com/_/solr).

The following command sets up a new Solr instance, starts it (standalone, on http://localhost:8983/), and creates a new index (core) called `lasso_quickstart`:

```bash
# create a directory to store the server/solr directory
mkdir lassoindex

# make sure its host owner matches the container's solr user
sudo chown -R 8983:8983 lassoindex

# creates and runs a solr container (localhost:8983, creates a new index called 'lasso_quickstart'
docker run -d -v "$PWD/lassoindex:/var/solr" -p 8983:8983 --name lasso_solr_quickstart solr solr-precreate lasso_quickstart

# check if container is running (see STATUS column)
docker ps -a

# alternatively, open your webbrowser and load Solr's dashboard (http://localhost:8983/)
```

Note: tested under Ubuntu 22.04 LTS with Solr _9.4.1_ and docker _25.0.3_.

Now update the schema of your newly created index (assuming your Solr instance is running on _localhost_ on port _8983_)

```bash
# copy LASSO document schema to your index
sudo cp solr_config/managed-schema* lassoindex/data/lasso_quickstart/conf/

# make sure its host owner matches the container's solr user
sudo chown -R 8983:8983 lassoindex/data/lasso_quickstart/conf/

# reload the index (core) to load the new schema - 'curl' is required on your system
curl -vvv "http://localhost:8983/solr/admin/cores?action=RELOAD&core=lasso_quickstart"

# alternatively, open your webbrowser and load Solr's dashboard to reload the index using 'Core Admin' (http://localhost:8983/)
```

The schema files are located in [managed-schema](solr%2Fmanaged-schema).