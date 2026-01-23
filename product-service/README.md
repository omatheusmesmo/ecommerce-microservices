# Product Service

The Product Service is a microservice within the E-commerce Microservices architecture, responsible for managing product catalog operations. It provides RESTful APIs for CRUD operations on products, handles stock management, and integrates with other services via Kafka messaging for event-driven communication.

Built with Quarkus, this service leverages MongoDB for data persistence, Redis for caching, and Kafka for asynchronous messaging.

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- Docker (optional, for Dev Services or containerized deployment)

## Dependencies

The service uses the following key Quarkus extensions and libraries:

- **quarkus-mongodb-panache**: For MongoDB integration with Panache ORM
- **quarkus-redis-cache**: For Redis-based caching
- **quarkus-messaging-kafka**: For Kafka messaging
- **quarkus-rest-jackson**: For REST API with JSON serialization
- **quarkus-smallrye-health**: For health checks
- **quarkus-micrometer-registry-prometheus**: For metrics export
- **quarkus-hibernate-validator**: For input validation

## Business Rules

### Product Entity
- **Name**: Required, 2-100 characters
- **Description**: Optional, up to 500 characters
- **Price**: Required, greater than 0.01
- **Stock**: Required, non-negative integer
- **Category**: Required, 2-50 characters
- **Active**: Boolean flag, defaults to true
- **Timestamps**: Created and updated automatically

### Operations
- **Create**: Validates input, persists to MongoDB, publishes `product-created` event
- **Read**: Supports listing all, by ID, by category, and active products; uses Redis caching
- **Update**: Validates input, updates stock if changed (publishes `stock-changed` event), publishes `product-updated` event
- **Delete**: Removes product, publishes `product-deleted` event
- **Stock Management**: Decreases stock on order events (`order-created`), increases on restock; publishes `stock-changed` events

### Validation
All inputs are validated using Bean Validation annotations. Invalid requests return appropriate HTTP status codes.

### Caching
- Redis-backed caching for query results (TTL: 5-10 minutes)
- Cache invalidation on updates/deletes

## API Endpoints

Base path: `/products`

| Method | Endpoint | Description | Response |
|--------|----------|-------------|----------|
| GET | `/products` | Retrieve all products | 200 OK - List of products |
| GET | `/products/{id}` | Retrieve product by ID | 200 OK - Product or 404 Not Found |
| GET | `/products/category/{category}` | Retrieve products by category | 200 OK - List of products |
| GET | `/products/active` | Retrieve active products | 200 OK - List of products |
| POST | `/products` | Create a new product | 201 Created - Product |
| PUT | `/products/{id}` | Update an existing product | 200 OK - Product or 404 Not Found |
| DELETE | `/products/{id}` | Delete a product | 204 No Content or 404 Not Found |

All endpoints return JSON responses. POST and PUT require valid Product JSON in the request body.

## Kafka Topics

### Producers
- **product-created**: Published when a new product is created
- **product-updated**: Published when a product is updated
- **product-deleted**: Published when a product is deleted
- **stock-changed**: Published when product stock changes (purchase, restock, adjustment)

### Consumers
- **order-created**: Consumed to decrease stock for ordered products

Events use JSON serialization and include relevant product data.

## Running the Application

### Development Environment

For local development, use Quarkus Dev Services to automatically start MongoDB, Redis, and Kafka containers:

```bash
./mvnw compile quarkus:dev
```

This command:
- Starts the application in dev mode with hot reload
- Launches Dev Services containers for MongoDB, Redis, and Kafka
- Enables the Dev UI at http://localhost:8080/q/dev/

Access the application at http://localhost:8080.

### Production Environment

For production, ensure external services are available and configure via environment variables:

```bash
export QUARKUS_MONGODB_CONNECTION_STRING=mongodb://your-mongo-host:27017
export QUARKUS_MONGODB_DATABASE=your-database
export QUARKUS_REDIS_HOSTS=redis://your-redis-host:6379
export KAFKA_BOOTSTRAP_SERVERS=your-kafka-host:9092
./mvnw package -Dquarkus.package.type=uber-jar
java -jar target/*-runner.jar
```

## Observability

### Health Checks
- Endpoint: `/q/health`
- Provides readiness and liveness probes

### Metrics
- Endpoint: `/q/metrics`
- Exports Prometheus-compatible metrics for monitoring

### Logging
- Configurable levels via `application.properties`
- JSON format in production for log aggregation

## Building and Packaging

### Standard JAR
```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Uber JAR
```bash
./mvnw package -Dquarkus.package.type=uber-jar
java -jar target/*-runner.jar
```

### Native Executable
```bash
./mvnw package -Dnative
# Or with Docker:
./mvnw package -Dnative -Dquarkus.native.container-build=true
./target/product-service-1.0.0-SNAPSHOT-runner
```

## Configuration

Key configuration properties (see `application.properties`):

- Database: MongoDB connection and pool settings
- Cache: Redis hosts and TTL configurations
- Messaging: Kafka brokers and topic configurations
- Observability: Health and metrics paths
- Logging: Levels and formats

Environment variables can override defaults for different environments.
