# Deploy to SWT GitLab Container Registry (Public)

## Login (swtrepo.informatik.uni-mannheim.de:5050)

```bash
TOKEN=XXX
echo "$TOKEN" | docker login swtrepo.informatik.uni-mannheim.de:5050 -u mkessel --password-stdin
```

## LASSO Embedded

```bash
docker build -t swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-service-embedded:latest -f docker/service_embedded/Dockerfile .
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-service-embedded:latest
```

## Integrations (e.g., Nicad)

see Dockerfiles in `root/docker/integrations/`

```bash
docker build -t swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/nicad:5.2 .
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/nicad:5.2

docker build -t swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/nicad:6.2 .
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/nicad:6.2

docker build -t swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/nicad:6.2.1 .
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/nicad:6.2.1
```

## Services

### Nexus

Configure a nexus container and then commit the changes


```bash
docker pull sonatype/nexus3:3.79.0
docker run -d -p 8081:8081 --name lasso-nexus sonatype/nexus3

# configure
docker exec -it lasso-nexus bash
cat sonatype-work/nexus3/admin.password
# configure in repos in http://localhost:8081/ (lasso-deploy, lasso-web)
# see https://softwareobservatorium.github.io/web/docs/infrastructure/nexus

# push (NOT preconfigured)
docker stop lasso-nexus
docker commit lasso-nexus swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:3.79.0
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:3.79.0
docker tag swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:3.79.0 swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:latest
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:latest


# push preconfigured (including preconfigured volume)
docker exec -it lasso-nexus /bin/bash
mkdir -p /tmp/my_vol_backup
cp -a /nexus-data/. /tmp/my_vol_backup/
docker commit lasso-nexus swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:3.79.0
# commit (see docker/nexus/)
docker build -t swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus-preconfigured:3.79.0 .
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus-preconfigured:3.79.0
docker tag swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus-preconfigured:3.79.0 swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus-preconfigured:latest
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus-preconfigured:latest
```

### Solr

```bash
docker pull solr:9.8.1
docker run -d -p 8983:8983 --name lasso-solr solr:9.8.1

# configure
docker exec -it lasso-solr solr create -c lasso_quickstart
docker cp doc/solr_config/ lasso-solr:~/
docker exec -it lasso-solr bash
cp /~/* /var/solr/data/lasso_quickstart/conf/
curl -vvv "http://localhost:8983/solr/admin/cores?action=RELOAD&core=lasso_quickstart"

docker stop lasso-nexus

# push (NOT preconfigured)
docker commit lasso-solr swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:9.8.1
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:9.8.1
docker tag swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:9.8.1 swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:latest
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:latest

# push (preconfigured)
docker exec -it lasso-solr /bin/bash
mkdir -p /tmp/my_vol_backup
cp -a /var/solr/. /tmp/my_vol_backup/
docker commit lasso-solr swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:9.8.1
# commit
docker build -t swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr-preconfigured:9.8.1 .
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr-preconfigured:9.8.1
docker tag swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr-preconfigured:9.8.1 swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr-preconfigured:latest
docker push swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr-preconfigured:latest
```

## Pull

```bash
# original (not configured)
docker pull swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus:latest
docker pull swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr:latest

# integrations
docker pull swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/nicad:6.2.1

# LASSO embedded service
docker pull swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-service-embedded:latest

# services preconfigured for quickstart!
docker pull swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus-preconfigured:latest
docker pull swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr-preconfigured:latest
```

## Deploy

### Classic

```bash
# 1. start LASSO and set work directory to current directory
docker run -it --env DIND_SUPPORT_LIBS=$(pwd)/lasso-work/ --network host -v /var/run/docker.sock:/var/run/docker.sock -v $(pwd)/lasso-work/:/opt/lasso/work/ -v $(pwd)/ignite/:/opt/lasso/ignite/ swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-service-embedded:latest

## start executable corpus
# 2a) start artifact repository (http://localhost:8081/)
docker run -d -p 8081:8081 --name lasso-nexus swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-nexus-preconfigured:latest
# 2b) start code search index (http://localhost:8983/)
docker run -d -p 8983:8983 --name lasso-solr swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-solr-preconfigured:latest
```

### Compose

```bash
curl https://raw.githubusercontent.com/SoftwareObservatorium/lasso/refs/heads/develop/docker/compose/docker-compose-embedded.yml -o docker-compose.yml
docker compose up
```