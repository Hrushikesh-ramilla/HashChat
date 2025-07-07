FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline -q 2>/dev/null || true
COPY src ./src
RUN mvn -B -DskipTests -Dcheckstyle.skip=true package

FROM eclipse-temurin:17-jre
RUN addgroup --system --gid 1001 hashchat && \
    adduser --system --uid 1001 --ingroup hashchat hashchat
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
RUN chown hashchat:hashchat /app/app.jar
USER hashchat
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx512m} -jar /app/app.jar"]
