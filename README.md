# E-Commerce Microservices

A scalable, cloud-native e-commerce backend built with **Java 25** and **Quarkus**, featuring a microservices architecture with **Kafka** for event-driven communication.

## 🏗 Architecture Overview

The system is composed of loose-coupled microservices that communicate synchronously via REST APIs and asynchronously via Kafka messaging.

### Services

| Service | Technology | Port | Description |
| :--- | :--- | :--- | :--- |
| **Authentication Service** | Quarkus, PostgreSQL, Redis | `8080` | Manages identity, JWT issuance (ECC), registration, and RBAC. |
| **Order Service** | Quarkus, PostgreSQL, Outbox | `8081` | Manages order lifecycle with transactional outbox pattern for reliable event publishing. |
| **Product Service** | Quarkus, MongoDB, Redis | `8082` | Manages product catalog, stock levels, and category listings. |
| **Notification Service** | Quarkus, Kafka | `8083` | Consumes events from Kafka to send emails (Discord, Brevo). |
| **Kafka Connect** | Debezium | `8084` | CDC connector for capturing changes from outbox table and publishing to Kafka. |

### Infrastructure

| Component | Port | Purpose |
| :--- | :--- | :--- |
| **PostgreSQL** | `5432` | Order and Auth databases (with logical replication for CDC) |
| **PostgreSQL Auth** | `5433` | Authentication service database |
| **MongoDB** | `27017` | Product catalog database |
| **Redis** | `6379` | Caching layer |
| **Kafka** | `9092` (internal), `9093` (external) | Event streaming platform |
| **Zookeeper** | `2181` | Kafka coordination |
| **Kafka Connect** | `8084` | Debezium CDC connector |
| **Prometheus** | `9090` | Metrics collection |
| **Grafana** | `3000` | Monitoring dashboards |
| **Jaeger** | `16686` | Distributed tracing |

## 🎯 Key Features

### Transactional Outbox Pattern ✅
- **Order Service** uses Debezium CDC for guaranteed event delivery
- Events are persisted in the `outbox` table within the same transaction as business data
- Kafka Connect captures changes and publishes to Kafka topics
- **No event loss** even if Kafka is temporarily unavailable

### Event-Driven Architecture
- **Order.events** - Unified topic for all Order events (OrderCreated, OrderStatusChanged)
- **product-created** / **product-updated** / **product-deleted** - Product lifecycle events
- **stock-changed** - Inventory updates
- **authentication-email** - User registration and password recovery events

### Resilience & Fault Tolerance ✅
- **Dead Letter Queue (DLQ)**: Order and authentication-email events that fail to process are routed to a per-consumer DLQ topic instead of being dropped or retried indefinitely
- **Idempotent Consumers**: Order event consumers dedupe on `(topic, partition, offset)`, so Kafka redeliveries after a failed/retried message don't cause duplicate side effects
- **Timeouts & Circuit Breakers**: Discord and Brevo HTTP clients are bounded by `@Timeout`/`@CircuitBreaker`, so a hung downstream call can't tie up the notification service's shared worker pool
- **Enhanced Observability**: DEBUG-level logs for `com.ecommerce.*` in development, INFO in production
- **Native Image Reflection**: All DTOs properly configured for GraalVM native compilation
- **Zero Data Loss**: Events preserved in DLQ for manual investigation and reprocessing

## 🚀 Getting Started

### Prerequisites

- **Java 25+** (JDK)
- **Maven 3.8+**
- **Docker** & **Docker Compose**
- **OpenSSL** (for generating JWT security keys)

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd ecommerce-microservices
   ```

2. **Generate Security Keys (Required for Auth):**
   ```bash
   cd authentication-service
   # Generate ECC keys as described in authentication-service/README.md
   cd ..
   ```

3. **Start all services:**
   ```bash
   docker-compose up -d
   ```

4. **Verify services:**
   ```bash
   # Check service health
   curl http://localhost:8081/q/health  # Order Service
   curl http://localhost:8082/q/health  # Product Service
   curl http://localhost:8083/q/health  # Notification Service

   # Check Kafka Connect
   curl http://localhost:8084/

   # Check Debezium connector status
   curl http://localhost:8084/connectors/order-service-outbox-connector/status
   ```

### Testing the Outbox Pattern

```bash
# Run the automated integration test
cd order-service
./scripts/test-outbox-integration.sh
```

For detailed setup and testing instructions, see:
- **[OUTBOX-QUICK-START.md](OUTBOX-QUICK-START.md)** - Quick start guide
- **[order-service/KAFKA-CONNECT-SETUP.md](order-service/KAFKA-CONNECT-SETUP.md)** - Complete setup guide

## 🐳 Running with Docker

### Standard Startup

```bash
# Start all services (infrastructure + applications)
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f order-service
docker-compose logs -f kafka-connect

# Stop all services
docker-compose down

# Stop and remove all data volumes (clean state)
docker-compose down -v
```

### Sequential Startup (For Resource-Constrained Systems)

Building multiple Quarkus services in **native mode** simultaneously is resource-intensive. Use the sequential startup script:

```bash
# Build and start everything sequentially
./sequential-up.sh --build

# Just start containers sequentially
./sequential-up.sh up
```

This script starts infrastructure first, waits for stabilization, then builds/starts each app one by one.

### Monitoring Services

```bash
# Check running containers
docker-compose ps

# Check Kafka Connect health
curl http://localhost:8084/

# Check Debezium connector status
curl http://localhost:8084/connectors/order-service-outbox-connector/status | jq '.connector.state'
# Expected: "RUNNING"

# View outbox table
docker exec -it ecommerce-postgres psql -U admin -d orderdb \
  -c "SELECT * FROM outbox ORDER BY created_at DESC LIMIT 5;"

# Consume Kafka events
docker exec -it ecommerce-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic Order.events \
  --from-beginning
```

### Running Services Individually (Development)

For debugging, you can run services with Quarkus Dev Mode:

**Authentication Service:**
```bash
cd authentication-service
./mvnw quarkus:dev
```

**Order Service:**
```bash
cd order-service
./mvnw quarkus:dev
```

**Product Service:**
```bash
cd product-service
./mvnw quarkus:dev
```

**Notification Service:**
```bash
cd notification-service
./mvnw quarkus:dev
```

## 📡 Event-Driven Architecture

### Outbox Pattern (Order Service)

The **Order Service** implements the **Transactional Outbox Pattern** using Debezium CDC:

```
Order Service
    ↓ (Transactional Write)
PostgreSQL Outbox Table
    ↓ (CDC - Change Data Capture)
Debezium Connector (Kafka Connect)
    ↓ (EventRouter SMT)
Kafka Topic: Order.events
    ↓ (Consume)
├─→ Notification Service (Email/Discord)
└─→ Product Service (Stock management)
```

**Benefits:**
- ✅ Guaranteed event delivery (at-least-once semantics)
- ✅ Transactional consistency between database and events
- ✅ No event loss even if Kafka is temporarily unavailable
- ✅ Automatic retry and recovery

### Kafka Topics

| Topic | Producer | Consumers | Event Types | Resilience |
|-------|----------|-----------|-------------|------------|
| **outbox.event.Order** | Debezium (from outbox) | Notification, Product | OrderCreated, OrderStatusChanged | Idempotent consumer + DLQ on failure |
| **outbox.event.Order.dlq** | Notification Service | DeadLetterQueueConsumer | Failed events | Manual investigation |
| **outbox.event.Order.product-service.dlq** | Product Service | OrderEventDeadLetterConsumer | Failed events | Manual investigation |
| **product-created** | Product Service | Notification | ProductCreatedEvent | Default |
| **product-updated** | Product Service | - | ProductUpdatedEvent | Default |
| **product-deleted** | Product Service | - | ProductDeletedEvent | Default |
| **stock-changed** | Product Service | Notification | StockChangedEvent | Default |
| **authentication-email** | Auth Service | Notification | Account activation/reset links, activation/reset confirmations | DLQ on failure |
| **authentication-email.dlq** | Notification Service | DeadLetterQueueConsumer | Failed events | Manual investigation |

**Note on outbox.event.Order topic**:
- Topic prefix: `outbox` (required by Debezium)
- Database server name: `outbox` (used in topic construction)
- Aggregate type: `Order` (from outbox table)
- Route replacement: `outbox.event.${routedByValue}`
- Final topic: `outbox.event.Order`
- Consumers automatically handle double-encoded JSON from Debezium

**Resilience Strategy**:
- **Idempotent Consumers**: Order event consumers record `(topic, partition, offset)` as processed after a successful run, so a message redelivered by Kafka (e.g. after a transient failure) doesn't get processed twice
- **Dead Letter Queue (DLQ)**: Events that fail to process are routed to a per-service DLQ topic for investigation
- **No Data Loss**: Events are preserved in DLQ for manual reprocessing or troubleshooting

### Direct Publishing (Product Service)

The Product Service still uses direct Kafka publishing for its events. Future improvements may migrate it to the Outbox Pattern as well.

## 🛠 Tech Stack

- **Framework:** Quarkus (Super-fast Subatomic Java)
- **Language:** Java 25
- **Databases:** PostgreSQL (with logical replication), MongoDB
- **Caching:** Redis
- **Messaging:** Apache Kafka
- **CDC:** Debezium + Kafka Connect
- **Security:** SmallRye JWT (ECC), Argon2
- **Observability:** Prometheus, Grafana, Jaeger (OpenTelemetry)
- **Containerization:** Docker

## 📊 Monitoring & Observability

### Health Checks

```bash
curl http://localhost:8081/q/health  # Order Service
curl http://localhost:8082/q/health  # Product Service
curl http://localhost:8083/q/health  # Notification Service
curl http://localhost:8080/q/health  # Auth Service
```

### Metrics (Prometheus)

```bash
curl http://localhost:8081/q/metrics  # Order Service metrics
```

Access Prometheus UI: http://localhost:9090

### Dashboards (Grafana)

Access Grafana: http://localhost:3000
- User: admin
- Password: (check .env file)

### Distributed Tracing (Jaeger)

Access Jaeger UI: http://localhost:16686

## 🤝 Contribution

1. Fork the project.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## 📝 License

This project is licensed under the [PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0) - see the LICENSE file for details. Noncommercial use (personal, educational, research) is permitted; commercial use requires a separate license from the author.
