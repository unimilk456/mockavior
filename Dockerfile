FROM eclipse-temurin:21-jre

RUN useradd -r -u 1001 mockavior

WORKDIR /app

COPY /build/libs/mockavior.jar /app/mockavior.jar

ENV MOCKAVIOR_CONTRACT_PATH=/app/config/mockapi.yml
ENV JAVA_OPTS=""

EXPOSE 8080

USER mockavior

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/mockavior.jar"]
