FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common/pom.xml common/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY shipment-service/pom.xml shipment-service/pom.xml
COPY customer-relations-service/pom.xml customer-relations-service/pom.xml
COPY streaming-service/pom.xml streaming-service/pom.xml
COPY e2e-tests/pom.xml e2e-tests/pom.xml

RUN chmod +x mvnw && ./mvnw -pl streaming-service -am dependency:go-offline

COPY common/src common/src
COPY streaming-service/src streaming-service/src

RUN ./mvnw -pl streaming-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ENV JAVA_OPTS=""

COPY --from=build /workspace/streaming-service/target/streaming-service-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
