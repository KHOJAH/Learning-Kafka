## Kafka Producer Configuration and Messaging Components

### Configuration Classes

**`KafkaTopicConfig.java`** This class manages the lifecycle of the Kafka topics required by the application.

- **Main Topics (9):** `order-created`, `order-confirmed`, `order-cancelled`, `payment-processed`, `payment-failed`, `inventory-reserved`, `inventory-released`, `notification-email`, and `notification-sms`.

- **Dead Letter Topics (DLT):** Dedicated topics for handling failed messages from `order-created` and `payment-processed`.

- **Specifications:** All topics are configured with **3 partitions** and **1 replica**.


**`KafkaProducerConfig.java`** The central configuration for establishing connections and defining how data is transmitted.

- **Connectivity:** Defines the bootstrap server addresses.

- **Serialization:** Configures the conversion of Java objects into JSON format for transmission.

- **Reliability:** Manages acknowledgments (acks), retry logic, and data compression settings.

- **Producer Factories:** Provides two distinct factories: one for standard messaging and one specifically for **transactional messaging**.

- **KafkaTemplate Beans:** Exposes the templates used by the producer classes to send messages.


---

### Producer Implementations

**`PaymentProducer.java`** Handles the emission of payment-related events.

- Dispatches `payment processed` and `payment failed` events.

- **Transactions:** Supports atomic operations, ensuring that both payment and order confirmation events succeed or fail as a single unit.

- **Monitoring:** Logs success/failure metadata, including partition and offset details.


**`InventoryProducer.java`** Manages events related to stock levels and reservations.

- Dispatches `inventory reserved` and `inventory released` events.

- **Monitoring:** Logs send results including the inventory ID, status, and Kafka-specific metadata (partition/offset).


**`NotificationProducer.java`** Triggers outgoing communication events.

- Dispatches events for both **Email** and **SMS** notifications.

- **Monitoring:** Logs send results including the notification ID, type, and Kafka-specific metadata.