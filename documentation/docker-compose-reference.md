# Docker Compose Reference

## Profiles

- `infra`: Kafka, MailHog, Kafka UI
- `full`: Everything in `infra` plus MySQL and all Spring services

## Services And Ports

| Service | Profile | Host Port(s) | Container Port(s) | Notes |
|---|---|---:|---:|---|
| Kafka | `infra`, `full` | `9092`, `29092` | `9092`, `29092` | `9092` is the host-facing listener, `29092` is used by other containers |
| MailHog | `infra`, `full` | `1025`, `8025` | `1025`, `8025` | SMTP on `1025`, web UI on `8025` |
| MySQL | `full` | `3307` | `3306` | Host port changed to `3307` to avoid conflicts with local MySQL |
| Order Service | `full` | `8081` | `8080` | Spring profile `docker` |
| Inventory Service | `full` | `8082` | `8080` | Spring profile `docker` |
| Shipment Service | `full` | `8083` | `8080` | Spring profile `docker` |
| Customer Relations Service | `full` | `8084` | `8080` | Spring profile `docker` |
| Kafka UI | `infra`, `full` | `8085` | `8080` | Kafka browser UI |

## Useful URLs

### Infrastructure

- Kafka UI: [http://localhost:8085](http://localhost:8085)
- MailHog UI: [http://localhost:8025](http://localhost:8025)

### Service UIs And Endpoints

- Order Service Swagger UI: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- Order Service OpenAPI: [http://localhost:8081/api-docs](http://localhost:8081/api-docs)
- Order Service Health: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)

- Inventory Service Swagger UI: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- Inventory Service OpenAPI: [http://localhost:8082/api-docs](http://localhost:8082/api-docs)
- Inventory Service Health: [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)

- Shipment Service Swagger UI: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)
- Shipment Service OpenAPI: [http://localhost:8083/api-docs](http://localhost:8083/api-docs)
- Shipment Service Health: [http://localhost:8083/actuator/health](http://localhost:8083/actuator/health)

- Customer Relations Service Swagger UI: [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)
- Customer Relations Service OpenAPI: [http://localhost:8084/api-docs](http://localhost:8084/api-docs)
- Customer Relations Service Health: [http://localhost:8084/actuator/health](http://localhost:8084/actuator/health)

## Database Access

Use these settings for tools such as DataGrip:

- Host: `localhost`
- Port: `3307`
- User: `kafka_challenge`
- Password: `kafka_challenge`

Available schemas:

- `order_service`
- `inventory_service`
- `shipment_service`
- `customer_relations_service`

## Useful Docker Compose Commands

### Start Infra Only

Starts Kafka, MailHog, and Kafka UI in the background.

```bash
docker compose --profile infra up -d
```

### Start Full Stack

Starts the full environment in the background.

```bash
docker compose --profile full up -d
```

### Start Full Stack And Rebuild App Images

Useful after code changes that should be reflected in the containers.

```bash
docker compose --profile full up -d --build
```

### Stop Containers

Stops the containers but keeps networks and volumes.

```bash
docker compose --profile full stop
```

### Remove Containers And Network

Stops and removes the stack containers and network. Named volumes remain unless explicitly removed.

```bash
docker compose --profile full down
```

### View Running Containers

Shows container state and exposed ports.

```bash
docker compose ps
```

### Follow Logs

Streams logs for the whole stack.

```bash
docker compose logs -f
```

### Follow Logs For One Service

Useful for debugging startup or healthchecks.

```bash
docker compose logs -f kafka
```

### Recreate One Service

Useful after changing only one service definition in `docker-compose.yaml`.

```bash
docker compose up -d kafka
```

### Remove A Broken Service Container

Useful if a single service is stuck in a partially created state.

```bash
docker compose --profile full rm -f mysql
docker compose --profile full up -d
```
