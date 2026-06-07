# order-service

Manages the full lifecycle of customer orders within the e-commerce platform.

## Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Getting Started](#getting-started)
5. [Configuration](#configuration)
6. [API Endpoints](#api-endpoints)
7. [Inter-Service Communication](#inter-service-communication)
8. [Database](#database)
9. [Running Tests](#running-tests)
10. [Docker](#docker)
11. [Environment Variables](#environment-variables)

---

## Overview

`order-service` is responsible for creating, retrieving, updating, and deleting orders placed by customers. It owns the `orders` and `order_items` tables and manages order status transitions (`PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED` / `CANCELLED`). It does **not** own product data, user/customer records, or authentication state — those concerns belong to upstream services.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL |
| HTTP Client | Spring `RestTemplate` |
| Security / Auth | JWT (jjwt 0.12.5), custom `HandlerInterceptor` |
| Validation | Spring Boot Validation (Jakarta Bean Validation) |
| Build Tool | Maven (Maven Wrapper) |
| Utilities | Lombok |

---

## Architecture

`order-service` is a standalone REST microservice running on port **8083**. It sits downstream of an authentication/gateway layer that issues JWTs, and upstream of no other internal service. On every request it validates the Bearer JWT locally; write operations (`POST`, `PUT`, `DELETE`) additionally require the `ADMIN` role. When an order is created it calls `product-service` synchronously via `RestTemplate` to fetch product details and decrement stock; when an order is cancelled/deleted it increments stock back. There is no Kafka or other async messaging in this service.

```
Client / API Gateway
        │  Bearer JWT
        ▼
  order-service (:8083)
        │  REST (RestTemplate)
        ▼
  product-service (:8080)
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+ (or use the included `mvnw` wrapper)
- MySQL 8+ instance accessible at `localhost:3306`
- A running `product-service` instance (default: `http://localhost:8080`)
- A valid `JWT_SECRET_KEY` environment variable (same secret used to sign tokens)

### Clone, Build, and Run

```bash
# Clone the repository
git clone <repository-url>
cd order-service

# Create the database (MySQL)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS order_service_db;"

# Build (skip tests if dependencies are not yet running)
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run
```

---

## Configuration

Configuration is managed through `src/main/resources/application.properties`. Profile-specific overrides use the naming convention `application-{profile}.properties` (e.g., `application-dev.properties` is present in the compiled output). Activate a profile at runtime:

```bash
# Development profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Production profile
java -jar target/order-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Key properties to review/override before running in any environment: `spring.datasource.*`, `product.service.base-url`, and `jwt.secret-key`.

---

## API Endpoints

All endpoints are prefixed with `/orders`. Every request must include a valid `Authorization: Bearer <token>` header. Write operations (`POST`, `PUT`, `DELETE`) additionally require the `ADMIN` role.

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/orders` | Create a new order | Yes (ADMIN) |
| `GET` | `/orders` | Retrieve all orders | Yes |
| `GET` | `/orders/{id}` | Retrieve a single order by ID | Yes |
| `GET` | `/orders/customer/{customerId}` | Retrieve all orders for a customer | Yes |
| `PUT` | `/orders/{id}/status` | Update the status of an order | Yes (ADMIN) |
| `DELETE` | `/orders/{id}` | Delete / cancel an order | Yes (ADMIN) |

---

## Inter-Service Communication

| Communicates With | Protocol | Purpose |
|---|---|---|
| `product-service` | REST (`RestTemplate`) | Fetch product details (name, price, availability) when creating an order |
| `product-service` | REST (`RestTemplate`) | Decrement product stock on order creation |
| `product-service` | REST (`RestTemplate`) | Increment product stock on order deletion / cancellation |

---

## Database

- **Type:** MySQL 8
- **Database name:** `order_service_db`
- **Schema management:** Hibernate DDL auto (`spring.jpa.hibernate.ddl-auto=update`) — no separate migration tool (Flyway/Liquibase) is configured.

### Main JPA Entities

| Entity | Table | Description |
|---|---|---|
| `Order` | `orders` | Top-level order record; holds `orderNumber`, `customerId`, `totalAmount`, and `orderStatus` |
| `OrderItem` | `order_items` | Line item linked to an `Order`; holds `productId`, `quantity`, `price`, and `totalAmount` |
| `BaseModel` | _(mapped superclass)_ | Provides `id` (auto-increment), `createdAt`, and `lastUpdatedAt` audit fields for all entities |

---

## Running Tests

No JaCoCo plugin is configured in `pom.xml`.

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=OrderServiceApplicationTests

# Run integration tests only (if Surefire failsafe is configured)
./mvnw verify
```

> **Note:** Tests that exercise `OrderController` or `ProductServiceClient` require a running MySQL instance and `product-service`, or appropriate mocks/test-containers.

---

## Docker

No `Dockerfile` or `docker-compose.yml` is present in this repository. The following commands demonstrate a standard Spring Boot containerisation approach:

```bash
# Build the JAR first
./mvnw clean package -DskipTests

# Build the Docker image
docker build -t order-service:latest .

# Run the container
docker run -p 8083:8083 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/order_service_db \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e PRODUCT_SERVICE_BASE_URL=http://product-service:8080 \
  -e JWT_SECRET_KEY=<your-secret> \
  order-service:latest
```

---

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `JWT_SECRET_KEY` | Secret key used to verify incoming JWT tokens. Must match the key used by the auth/token-issuing service. | `c2VjcmV0a2V5MTIzNDU2Nzg5MA==` |
| `SPRING_DATASOURCE_URL` | JDBC URL for the MySQL database (overrides `application.properties`). | `jdbc:mysql://localhost:3306/order_service_db` |
| `SPRING_DATASOURCE_USERNAME` | MySQL username (overrides `application.properties`). | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password (overrides `application.properties`). | `root` |
| `PRODUCT_SERVICE_BASE_URL` | Base URL of the product-service used by `ProductServiceClient` (maps to `product.service.base-url`). | `http://localhost:8080` |

