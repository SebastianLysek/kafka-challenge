# Kafka Challenge

Small multi-module Spring Boot project that explores Kafka-based communication between
microservices. The repository contains several services plus shared code in the
`common` module.

## Modules
- `common` shared types and utilities
- `order-service` order domain service
- `inventory-service` inventory domain service
- `shipment-service` shipment domain service
- `customer-relations-service` customer relations domain service

## Tech stack
- Java 21
- Spring Boot
- Spring Kafka
- Maven
- Docker Compose (local Kafka)

## Getting started
Prerequisites:
- Java 21
- Maven
- Docker (optional, for local Kafka)

Build all modules:
```bash
mvn clean package
```

Start local Kafka (optional):
```bash
docker compose up -d
```

Docker Compose (infra profile):
- Start existing containers/networks/volumes:
```bash
docker compose --profile infra start
```
- Stop containers (keeps networks/volumes):
```bash
docker compose --profile infra stop
```
- Recreate containers and networks, keep topics/data:
```bash
docker compose --profile infra down
docker compose --profile infra up -d
```

Run a service:
```bash
mvn -pl order-service spring-boot:run
```

## Tests
Run all tests:
```bash
mvn test
```

Run a single module's tests:
```bash
mvn -pl order-service test
```
