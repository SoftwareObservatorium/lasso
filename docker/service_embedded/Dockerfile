# FIXME finalize
FROM eclipse-temurin:17-jdk-jammy

# for docker in docker via socket
RUN apt update && curl -fsSL https://get.docker.com | sh

RUN mkdir /opt/lasso
COPY service/target/service-1.0.0-SNAPSHOT.jar /opt/lasso/service-1.0.0-SNAPSHOT.jar

# copy default config
RUN mkdir /opt/lasso/config
COPY doc/lasso_config/users.json /opt/lasso/config
COPY doc/lasso_config/corpus.json /opt/lasso/config

# copy arena to temp dir
RUN mkdir -p /opt/lasso_temp/support
COPY arena/target/arena-1.0.0-SNAPSHOT-exec.jar /opt/lasso_temp/support/arena-1.0.0-SNAPSHOT.jar

#ENV TEST2=$TEST1

# going to be host mount
WORKDIR /opt/lasso/

# create support dir, populate arena, start service
CMD mkdir -p /opt/lasso/work/repository/support/ && \
    cp /opt/lasso_temp/support/arena-1.0.0-SNAPSHOT.jar /opt/lasso/work/repository/support/ && \
    chmod -R 777 /opt/lasso_temp/support/ && \
    java --add-opens java.base/java.nio=ALL-UNNAMED \
    --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED \
    --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED \
    --add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED \
    --add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED \
    --add-opens=java.base/java.io=ALL-UNNAMED \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    --add-opens=java.base/java.util=ALL-UNNAMED \
    --add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
    --add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    -server -ea -Xms2G -Xmx4G \
    -Dserver.port=10222 -Djava.net.preferIPv4Stack=true -Dcluster.nodeId=lasso-quickstart -Dcluster.embedded=true \
    -Dthirdparty.docker.uid=$(id -u) -Dthirdparty.docker.gid=$(id -g) \
    -Dlasso.workspace.root=/opt/lasso/work/ \
    -Dusers="file:/opt/lasso/config/users.json" \
    -Dcorpus="file:/opt/lasso/config/corpus.json" \
    -jar /opt/lasso/service-1.0.0-SNAPSHOT.jar
