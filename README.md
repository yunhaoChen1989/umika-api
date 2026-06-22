# Umika API

Spring Boot backend for the Umika Sushi platform.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 15+

## Run Locally

For Swagger/API development before PostgreSQL is provisioned, use the local H2-backed profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

For the normal PostgreSQL-backed profile:

```bash
mvn spring-boot:run
```

By default the API expects PostgreSQL at:

```text
jdbc:postgresql://localhost:5432/umika
```

Override with environment variables:

```bash
DB_URL=jdbc:postgresql://localhost:5432/umika \
DB_USERNAME=umika \
DB_PASSWORD=umika \
mvn spring-boot:run
```

## Swagger UI

Once the server is running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Public ping: `http://localhost:8080/api/v1/public/ping`
- Actuator health: `http://localhost:8080/actuator/health`

## CRUD Layer

The API includes generated CRUD foundations for the current database schema:

- JPA database objects named `*Entity`
- API DTO records named `*Dto`
- Mapper classes named `*Mapper`
- Spring Data repositories named `*Repository`
- Transactional services named `*Service`
- REST controllers named `*Controller`

The generated resources use feature packages such as `user`, `menu`, `order`, `reward`, `referral`, `payment`, `email`, `admin`, and `store`.

## Module Style

Use feature-based modules:

- `auth`
- `user`
- `menu`
- `order`
- `reward`
- `referral`
- `payment`
- `email`
- `admin`
- `common`

Avoid root-level technical packages such as `controller`, `service`, and `repository`.
