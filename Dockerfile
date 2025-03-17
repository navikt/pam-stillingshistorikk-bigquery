FROM gcr.io/distroless/java21

COPY build/libs/stillingshistorikk-*-all.jar ./app.jar
ENV JAVA_OPTS="-Xms768m -Xmx1280m"
ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

ENTRYPOINT ["java", "-jar", "/app.jar"]