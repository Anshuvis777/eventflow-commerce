FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY eventflow-common/pom.xml eventflow-common/
COPY order-service/pom.xml order-service/
RUN mvn dependency:go-offline -pl order-service -am || true
COPY . .
RUN mvn clean package -DskipTests -pl order-service -am

FROM eclipse-temurin:21-jre-alpine
COPY docker/certs/aiven-ca.pem /tmp/aiven-ca.pem
RUN keytool -importcert -noprompt -trustcacerts -alias aiven-ca -file /tmp/aiven-ca.pem -keystore /opt/java/openjdk/lib/security/cacerts -storepass changeit && chmod 644 /opt/java/openjdk/lib/security/cacerts && rm /tmp/aiven-ca.pem
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
USER appuser
EXPOSE 8081
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=50"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
