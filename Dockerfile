FROM navikt/java:14
USER root
USER apprunner
COPY scripts/init-kafka-env.sh /init-scripts/init-kafka-env.sh
COPY build/libs/stihibi-*-all.jar ./app.jar
ENV JAVA_OPTS="-Xms256m -Xmx1024m"
