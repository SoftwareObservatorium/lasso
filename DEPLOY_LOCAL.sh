#/bin/sh
./mvnw -DskipTests   -Dfrontend.build=embedded   clean install
cp arena/target/arena-1.0.0-SNAPSHOT-exec.jar ~/lasso-work/repository/support/arena-1.0.0-SNAPSHOT.jar
