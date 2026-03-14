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
- Docker Compose

## Getting started
Prerequisites:
- Java 21
- Maven
- Docker (optional, for the local stack)

Build all modules:
```bash
mvn clean package
```

Start the full local stack (optional):
```bash
docker compose --profile full up -d
```

Docker Compose reference:
- See [documentation/docker-compose-reference.md](documentation/docker-compose-reference.md) for profiles, ports, URLs, database access, and common Docker commands.

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
