#/bin/sh
./mvnw -DskipTests   -Dfrontend.build=embedded   clean install
docker build -t lasso-service-embedded/latest -f docker/service_embedded/Dockerfile .
