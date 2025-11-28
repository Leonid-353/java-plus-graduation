FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
ARG JAR_FILE=stats-server/target/*.jar
COPY ${JAR_FILE} stats_server.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /stats_server.jar"]