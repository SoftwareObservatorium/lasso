#/bin/sh
./mvnw -DskipTests   -Dfrontend.build=embedded   clean install
#docker build -t lasso-service-embedded/latest -f docker/service_embedded/Dockerfile .
docker build -t swtrepo.informatik.uni-mannheim.de:5050/docker/lasso/lasso-service-embedded:latest -f docker/service_embedded/Dockerfile .
