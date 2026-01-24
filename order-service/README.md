# order-service

This is the order-service microservice for the e-commerce platform, built with Quarkus. It manages the lifecycle of customer orders, including creation, status updates, and cancellations. The service integrates with PostgreSQL for data persistence, Kafka for event publishing, and uses Flyway for database migrations.

## Tech Stack
- **Framework**: Quarkus 3.30.6
- **Database**: PostgreSQL with Hibernate ORM Panache
- **Messaging**: Kafka with SmallRye Reactive Messaging
- **Migrations**: Flyway
- **Validation**: Hibernate Validator
- **Observability**: SmallRye Health, Micrometer Prometheus
- **Build**: Maven

## Prerequisites
- Java 21+
- Maven 3.6+

## Business Rules

### Order Lifecycle
- Orders start in **PENDING** status upon creation.
- Statuses: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED.
- Total amount is calculated as the sum of item subtotals (quantity * unitPrice).
- Orders can be updated to any status, but cancellations are restricted:
  - Cannot cancel a **DELIVERED** order.
  - Cannot cancel an already **CANCELLED** order.

### Validation
- Customer name: 3-100 characters, required.
- Customer email: Valid email format, required.
- Items: At least one item required.
- Product name: 3-200 characters.
- Quantity: Minimum 1.
- Unit price: Greater than 0.

## API Endpoints

All endpoints return JSON responses. Use `Content-Type: application/json` for POST/PUT requests.

### Create Order
- **POST** `/orders`
- Body: `CreateOrderRequest`
- Creates a new order, calculates total, and publishes `order-created` event.

### List All Orders
- **GET** `/orders`
- Returns a list of orders without items.

### Get Order by ID
- **GET** `/orders/{id}`
- Returns full order details including items.

### Get Orders by Status
- **GET** `/orders/status/{status}`
- Status: PENDING, CONFIRMED, etc.

### Get Orders by Customer Email
- **GET** `/orders/customer/{email}`

### Update Order Status
- **PUT** `/orders/{id}/status`
- Body: `{"status": "CONFIRMED"}`
- Publishes `order-status-changed` event.

### Cancel Order
- **PATCH** `/orders/{id}/cancel`
- Publishes `order-status-changed` event.

## Kafka Topics

### order-created
Published when a new order is created.
```json
{
  "orderId": 1,
  "customerName": "João Silva",
  "customerEmail": "joao@example.com",
  "status": "PENDING",
  "totalAmount": 100.00,
  "items": [
    {
      "productId": "prod-123",
      "productName": "Produto Exemplo",
      "quantity": 2,
      "unitPrice": 50.00,
      "subtotal": 100.00
    }
  ],
  "createdAt": "2023-10-01T10:00:00"
}
```

### order-status-changed
Published on status updates or cancellations.
```json
{
  "orderId": 1,
  "customerEmail": "joao@example.com",
  "oldStatus": "PENDING",
  "newStatus": "CONFIRMED",
  "changedAt": "2023-10-01T10:05:00"
}
```

## Database Schema

### orders
- id (BIGINT, PK)
- customer_name (VARCHAR(255))
- customer_email (VARCHAR(255))
- status (VARCHAR(50))
- total_amount (DECIMAL(10,2))
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

### order_items
- id (BIGINT, PK)
- product_id (VARCHAR(255))
- product_name (VARCHAR(255))
- quantity (INTEGER)
- unit_price (DECIMAL(10,2))
- order_id (BIGINT, FK to orders.id)

Indexes on customer_email, status, order_id, product_id.

## Configuration

Key properties in `application.properties`:

- **Database**: `quarkus.datasource.*` for PostgreSQL connection.
- **Kafka**: `mp.messaging.outgoing.*` for producers, `kafka.bootstrap.servers` for brokers.
- **Flyway**: `quarkus.flyway.*` for migrations.
- **Profiles**: dev, test, prod with different settings (e.g., schema generation, logging).

## Running the Application

### Dev Mode
```bash
./mvnw compile quarkus:dev
```

### Production
```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Build
```bash
./mvnw package -Dnative
```

## Health and Metrics

- Health: `/q/health`
- Metrics: `/q/metrics`

## Error Handling

Handled via `GlobalExceptionMapper`:
- 400 Bad Request for validation/illegal operations.
- 404 Not Found for missing resources.
- 500 Internal Server Error for unexpected issues.
