# EventDriven-E-Commerce

A hands-on learning project that teaches Apache Kafka and event-driven architecture by building a complete e-commerce order processing system with Spring Boot.

---

## What This Project Is

This isn't your typical Kafka tutorial. It's a complete learning journey that takes you from Kafka basics to advanced enterprise patterns like Saga orchestration and the Outbox pattern.

You'll build an event-driven microservices platform where:
- Customers place orders via REST API
- Payments get processed asynchronously
- Inventory reserves automatically
- Notifications go out to customers
- All services talk through Kafka events
- Failures handle themselves gracefully with retries and compensation

---

## Why I Built This

I made this project to actually understand modern event-driven architecture, not just copy-paste code. Here's what I learned:

### Architecture & Design
- **Event-Driven Architecture**: How to decouple services using events instead of HTTP calls
- **Microservices Communication**: Async messaging patterns that don't break when things go wrong
- **Saga Pattern**: Managing distributed transactions without two-phase commit (which nobody uses anyway)
- **Outbox Pattern**: Making sure events actually get published after database transactions

### Kafka Deep Dive
- **Producer Patterns**: Acknowledgments, retries, idempotency, transactions
- **Consumer Patterns**: Consumer groups, partitioning, offset management
- **Error Handling**: Dead Letter Topics, retry with exponential backoff
- **Configuration**: Production-ready Kafka setup that won't crash under load

### Spring Boot Integration
- **Spring Kafka**: Annotations, templates, and configuration that actually work
- **Error Handling**: Spring's retry mechanisms and DLT support
- **Testing**: Integration tests with embedded Kafka containers (no mocking everything)

### Production Readiness
- **Monitoring**: Metrics, consumer lag tracking, health checks
- **Observability**: Correlation IDs, distributed tracing
- **Resilience**: Circuit breakers, bulkheads, timeout handling

---

## The Business Scenario: E-Commerce Platform

```
Customer → Order Service → Kafka Topics → Payment Service
                                    ↓
Customer ← Notification ← Inventory ← Payment Confirmed
```

### Event Flow
1. Order Created → `order-created` topic
2. Payment Processed → `payment-processed` topic
3. Inventory Reserved → `inventory-reserved` topic
4. Order Confirmed → `order-confirmed` topic
5. Notification Sent → `notification-email` topic

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker Desktop (for Kafka infrastructure)

### Run in 3 Steps

```bash
# 1. Start Kafka infrastructure (Kafka, Zookeeper, Schema Registry, UI)
docker-compose up -d

# 2. Run the Spring Boot application
mvn spring-boot:run

# 3. Create your first order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST123",
    "customerEmail": "test@example.com",
    "totalAmount": 99.99,
    "items": "ITEM1,ITEM2",
    "shippingAddress": "123 Main St"
  }'
```

### Access Tools
- **Grafana**: http://localhost:3000 - Dashboards and metrics visualization (login: admin/admin)
- **Kafka UI**: http://localhost:8090 - View topics, messages, and consumer groups
- **Schema Registry**: http://localhost:8081 - Manage message schemas

---

## Monitoring with Grafana

### Dashboard Access
1. Open http://localhost:3000
2. Login with credentials: `admin` / `admin`
3. Navigate to **Dashboards → Kafka Monitoring → Kafka Mastery Project**

### Pre-configured Dashboards
The project includes an auto-provisioned dashboard showing:
- **HTTP Request Metrics** - Total requests, P95 latency, request rate
- **JVM Metrics** - Memory usage by heap/non-heap areas
- **Kafka Consumer Metrics** - Records consumed, consumer lag by topic/partition

### Key Metrics to Watch

| Metric | Description | Query |
|--------|-------------|-------|
| HTTP Request Rate | Requests per second | `rate(http_server_requests_total[1m])` |
| P95 Latency | 95th percentile response time | `histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket[5m]))` |
| JVM Memory Used | Current memory consumption | `sum(jvm_memory_used_bytes)` |
| Kafka Consumer Lag | Messages behind latest | `spring_kafka_consumer_lag` |
| Kafka Consumer Rate | Messages processed per second | `rate(spring_kafka_consumer_records_consumed_total[1m])` |

### Adding Custom Panels
1. Click **+** → **Add** → **Panel**
2. Select **Prometheus** as data source
3. Write your PromQL query
4. Customize visualization type (graph, stat, gauge, etc.)

### Useful PromQL Queries

```promql
# HTTP error rate (4xx and 5xx responses)
sum(rate(http_server_requests_total{status=~"4..|5.."}[5m]))

# Kafka consumer lag across all topics
sum(spring_kafka_consumer_lag{application="kafka-mastery-project"})

# JVM GC pause time
rate(jvm_gc_pause_seconds_sum[5m])

# Active threads
jvm_threads_live_threads{application="kafka-mastery-project"}
```

---

## Consumer Modes

This project supports two different processing modes that you can switch between via configuration:

### Mode 1: Standard Consumer Mode (Default)
Uses independent consumers for each processing stage. Each consumer handles one step and publishes events for the next.

**Active Components:**
- `OrderConsumer` - Processes orders and initiates payments
- `PaymentConsumer` - Confirms payments and requests inventory
- `InventoryConsumer` - Reserves inventory and confirms orders
- `NotificationConsumer` - Sends notifications

**Flow:** Order → Payment → Inventory → Confirmation → Notification

### Mode 2: Saga Orchestrator Mode
Uses a central orchestrator that coordinates the entire transaction with compensation logic.

**Active Components:**
- `OrderSagaOrchestrator` - Central coordinator for payment and inventory
- Automatic compensation (rollback) on failures
- Timeout handling for incomplete transactions

**Flow:** Order → [Saga: Payment → Inventory] → Confirmation

### How to Switch Modes

Edit `src/main/resources/application.yml`:

```yaml
# Consumer Mode Configuration
# Set to 'saga' to enable OrderSagaOrchestrator (disables standard consumers)
# Set to 'standard' to enable standard consumers (disables saga orchestrator)
kafka:
  consumer:
    mode: standard  # Change to 'saga' to use saga orchestrator
```

**Or use command-line argument:**
```bash
# Run with standard consumers
mvn spring-boot:run -Dspring-boot.run.arguments="--kafka.consumer.mode=standard"

# Run with saga orchestrator
mvn spring-boot:run -Dspring-boot.run.arguments="--kafka.consumer.mode=saga"
```

The startup logs will show which mode is active.

---

## Learning Path (4 Weeks)

| Week | Focus | Topics |
|------|-------|--------|
| Week 1 | Kafka Fundamentals | Producers, Consumers, Topics, Serializers |
| Week 2 | Advanced Patterns | Retry, Error Handling, Dead Letter Topics |
| Week 3 | Event-Driven Architecture | Saga Pattern, Outbox Pattern, Distributed Transactions |
| Week 4 | Production Ready | Monitoring, Testing, Observability |

### Detailed Learning Plan
Check [LEARNING_PATH.md](LEARNING_PATH.md) for a day-by-day study guide with:
- Challenge checklists
- Theory readings
- Hands-on exercises
- Milestone projects

---

## Project Structure

```
src/main/java/com/learning/kafka/
├── config/              # Kafka configurations (producer, consumer, topics, error handling)
├── controller/          # REST API endpoints
├── consumer/            # Kafka consumers (order, payment, inventory, notification)
├── producer/            # Kafka producers (order, payment, inventory, notification)
├── service/             # Business logic and event publishers
├── saga/                # Saga orchestrator for distributed transactions
├── outbox/              # Outbox pattern implementation
├── model/               # Domain models (Order, Payment, Inventory, Notification)
└── dto/                 # Data transfer objects
```

---

## Documentation

| Document | Purpose |
|----------|---------|
| [KAFKA_CONCEPTS.md](docs/KAFKA_CONCEPTS.md) | Theory: Kafka fundamentals, patterns, and concepts |
| [CHEATSHEET.md](docs/CHEATSHEET.md) | Quick reference: Commands, configurations, and code snippets |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design: Architecture diagrams and data flow |
| [EVENT_FLOW_DIAGRAM.md](docs/EVENT_FLOW_DIAGRAM.md) | Visual flows: Mermaid diagrams for each producer-consumer pair |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common issues and solutions |
| [LEARNING_PATH.md](LEARNING_PATH.md) | Study guide: 4-week learning plan with challenges |

---

## Key Features

### What You'll Learn

| Category | Topics |
|----------|--------|
| Producer Patterns | Async sending, callbacks, idempotency, transactions, headers |
| Consumer Patterns | Consumer groups, partitioning, offset commits, idempotency |
| Error Handling | Retry with backoff, Dead Letter Topics, error classifiers |
| Saga Pattern | Orchestration, compensation, state management |
| Outbox Pattern | Transactional messaging, polling, reliability |
| Monitoring | Micrometer, Prometheus, consumer lag, metrics |
| Testing | Embedded Kafka, TestContainers, integration tests |

### Architecture Patterns

- **Event Sourcing**: State changes captured as events
- **CQRS**: Separate read and write models
- **Saga Orchestration**: Centralized coordination of distributed transactions
- **Outbox Pattern**: Reliable event publishing from database transactions
- **Idempotency**: Handle duplicate messages safely

---

## Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# Run specific test class
mvn test -Dtest=OrderIntegrationTest
```

---

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Programming language |
| Spring Boot 3.x | Application framework |
| Spring Kafka | Kafka integration |
| Apache Kafka 3.5 | Message broker |
| Docker Compose | Infrastructure orchestration |
| Lombok | Code generation |
| H2 Database | In-memory database (dev) |
| SQL Server | Production database (Docker) |

---

## Next Steps After This Project

1. **Build Your Own Project**: Apply these patterns to a real-world use case
2. **Explore Kafka Streams**: Learn stream processing with Kafka Streams API
3. **Learn Schema Registry**: Use Avro schemas for type-safe messages
4. **Study Event Sourcing**: Deep dive into event-sourced architectures
5. **Explore Related Tech**: Redis, Elasticsearch, Apache Flink

---

## Additional Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Confluent Developer](https://developer.confluent.io/)
- [Kafka: The Definitive Guide (O'Reilly)](https://www.confluent.io/resources/kafka-the-definitive-guide-second-edition/)

## License

This project is for educational purposes. Feel free to use it for your own learning.
Created for learning Apache Kafka and event-driven architecture
