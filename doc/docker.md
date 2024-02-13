# Docker

Running LASSO in docker requires docker-in-docker (DIND) functionality, since LASSO uses docker for code execution.

Since DIND uses the host's docker socket, any image available on the host is also available inside docker containers. The mounting of volumes requires special attention. Mounting host volumes is realized based on this https://stackoverflow.com/a/62413225.

## LASSO Service (standalone mode)

1. Build the docker image in the root of repository using this [Dockerfile](..%2Fdocker%2Fservice_embedded%2FDockerfile)

```bash
docker build -t lasso-service-embedded/latest -f docker/service_embedded/Dockerfile .
```

2. Run LASSO service (standalone) (docker in docker )

```bash
# create LASSO work directory
mkdir lasso-work
# set environmental variable
LASSO_WORK_PATH=$(pwd)/lasso-work/

# start LASSO service (standalone) - interactive mode (to see logs etc.)
docker run -it \
  --env DIND_SUPPORT_LIBS=$LASSO_WORK_PATH \
  --network="host" \
  -p 10222:10222 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $LASSO_WORK_PATH:/opt/lasso/work/ \
  lasso-service-embedded/latest
```

Here is a short explanation of each argument

```
--env DIND_SUPPORT_LIBS=$LASSO_WORK_PATH # the host path is read by LASSO to detect if it is running in DIND
--network="host" # host network is required to talk to Solr and Nexus OSS
-p 10222:10222 # exposes LASSO's webservice to localhost:10222
-v /var/run/docker.sock:/var/run/docker.sock # required for DIND
-v $LASSO_WORK_PATH:/opt/lasso/work/ # mounts the host work dir in which LASSO stores executions
lasso-service-embedded/latest # the image we have created in the first step
```

## LASSO Actions

Some of LASSO's actions require certain docker images. To work in LASSO, the images have to be available on both the machines / containers (docker-in-docker) on which LASSO's service (manager) and workers run.

_Dockerfiles_ for LASSO integrations are located in [integrations](..%2Fdocker%2Fintegrations).

### Nicad - code clone detector

Here is an example to build Nicad 6.2.1

```bash
# go to Dockerfile
cd docker/integrations/nicad-6.2.1
# Build
docker image build -t nicad:6.2.1 .
```