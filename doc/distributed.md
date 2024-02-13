# Distributed Mode (large scale)

The distributed architecture of the platform is based on a manager/worker architecture.

There is one manager node that orchestrates the execution of LSL script pipelines. There must be at least one worker node in `distributed` mode.

Add as many worker nodes as suitable for your planned workload. Worker nodes do the actual execution of code and obtain tracing information etc.

## Manager Node (docker)

A LASSO cluster consists of exactly one manager node and one or more worker nodes. The exact list of nodes needs to be known beforehand (cf. `LASSO_NODES`).

Build the manager docker image based on this [Dockerfile](..%2Fdocker%2Fservice_cluster%2FDockerfile) as follows (in root directory)

```bash
docker build -t lasso-manager/latest -f docker/service_cluster/Dockerfile .
```

Then execute a manager as follows

```bash
# create LASSO work directory
mkdir lasso-work
# set environmental variable
LASSO_WORK_PATH=$(pwd)/lasso-work/

# start LASSO manager node - interactive mode (to see logs etc.)
docker run -it \
  --env DIND_SUPPORT_LIBS=$LASSO_WORK_PATH \
  --network="host" \
  -p 10222:10222 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $LASSO_WORK_PATH:/opt/lasso/work/ \
  -e LASSO_MANAGER_NODE_ID='manager' \
  -e LASSO_MANAGER_NODE_IP='managerip' \
  -e LASSO_NODES='m1,w1,w2,w3' \
  -e LASSO_CLUSTER_MULTICAST='228.1.2.6' \
  -e LASSO_MAVEN_THREADS=8 \
  lasso-manager/latest
```

Note that the manager node exposes a webservice on port 10222.

## Worker Nodes (docker)

A LASSO cluster needs at least one worker node.

Build the worker docker image based on this [Dockerfile](..%2Fdocker%2Fworker_cluster%2FDockerfile) as follows (in root directory)

```bash
docker build -t lasso-worker/latest -f docker/worker_cluster/Dockerfile .
```

Then execute a worker node as follows

```bash
# create LASSO work directory
mkdir lasso-work
# set environmental variable
LASSO_WORK_PATH=$(pwd)/lasso-work/

# start LASSO manager node - interactive mode (to see logs etc.)
docker run -it \
  --env DIND_SUPPORT_LIBS=$LASSO_WORK_PATH \
  --network="host" \
  -p 9988:9988 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $LASSO_WORK_PATH:/opt/lasso/work/ \
  -e LASSO_WORKER_NODE_ID='worker1' \
  -e LASSO_WORKER_NODE_IP='worker1ip' \
  -e LASSO_NODES='m1,w1,w2,w3' \
  -e LASSO_CLUSTER_MULTICAST='228.1.2.6' \
  -e LASSO_MAVEN_THREADS=8 \
  lasso-worker/latest
```

Note that worker nodes expose a webservice on port 9988.