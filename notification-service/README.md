# Notification Service

This service is responsible for sending notifications in an e-commerce system, using Kafka events to trigger notifications via Discord and email.

## Technologies

- **Java 17+**
- **Quarkus** (main framework)
- **Kafka** (messaging)
- **Discord Webhook** (Discord notifications)
- **Brevo (Sendinblue)** (email)
- **Maven** (dependency management)

## Features

- Consuming Kafka events for notifications on orders, products, and stock.
- Sending notifications via Discord (webhook) and email (Brevo API).
- Profile-based configuration (dev/prod) to enable/disable notifications.

## Configuration

### application.properties

Main configurations in `src/main/resources/application.properties`:

- **HTTP Port**: `quarkus.http.port=8080`
- **Discord**:
    - `discord.webhook.url`: Webhook URL (environment variable `DISCORD_WEBHOOK_URL`)
    - `discord.webhook.enabled`: Enables webhook (default: true)
    - `notification.discord.enabled`: Enables Discord notifications (default: true, disabled in dev)
- **Email (Brevo)**:
    - `brevo.api.key`: API key (environment variable `BREVO_API_KEY`)
    - `brevo.api.url`: API URL (default: https://api.brevo.com/v3)
    - `brevo.sender.name`: Sender name (default: BestEcommerce)
    - `brevo.sender.email`: Sender email (default: noreply@example.com)
    - `notification.email.enabled`: Enables email notifications (default: false)
- **Kafka**:
    - `kafka.bootstrap.servers`: Kafka servers (default: localhost:9093)
    - Dev services: `%dev.quarkus.kafka.devservices.enabled=true` (starts local Kafka in dev)

### Profiles

- **Dev**: Notifications disabled by default, Kafka dev services enabled.
- **Prod**: Notifications enabled, JSON logs.

## Consumed Kafka Topics

- `order-created`: Order created event.
- `order-status-changed`: Order status change event.
- `product-created`: Product created event.
- `stock-changed`: Stock change event.

## Endpoints

This service is event-driven and does not expose public REST endpoints. It consumes Kafka messages and sends notifications.

## How to Run

1. **Prerequisites**: Java 21+, Maven.
2. **Environment Variables** (optional for dev):
    - `DISCORD_WEBHOOK_URL`
    - `BREVO_API_KEY`
    - `BREVO_SENDER_NAME`
    - `BREVO_SENDER_EMAIL`
3. **Run in dev**:
   ```bash
   mvn quarkus:dev
   ```
    - Starts local Kafka automatically.
    - Notifications disabled to avoid external calls.

4. **Run in prod**:
   ```bash
   mvn quarkus:dev -Dquarkus.profile=prod
   ```

## Observability

- **Health Check**: `/q/health`
- **Metrics**: `/q/metrics` (Prometheus)
- **Logs**: INFO level, DEBUG for `com.ecommerce` in dev.

## Development

- **Code Structure**:
    - `provider/`: Notification providers (Discord, Email).
    - `client/`: Clients for external APIs (Discord, Brevo).
    - `dto/`: Data transfer objects.
    - `service/`: Business logic (NotificationService).
    - `event/`: Kafka events.

- **Tests**: Run `mvn test` for unit tests.

For more details, refer to the source code.