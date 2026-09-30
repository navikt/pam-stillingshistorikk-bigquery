FROM gcr.io/distroless/java21:latest@sha256:26a517c7f7d69a98adab4d1e71d5a3a9f1079c85ac9c4193ce6b6bd3d73496f3

COPY build/libs/stillingshistorikk-*-all.jar ./app.jar
ENV JAVA_OPTS="-Xms768m -Xmx1280m"
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

ENTRYPOINT ["java", "-jar", "/app.jar"]