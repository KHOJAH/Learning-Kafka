# Event Flow Diagrams - Divide and Conquer

This document provides detailed Mermaid flow diagrams for each producer-consumer pair in the Kafka-based e-commerce order processing system.

---

## Table of Contents

1. [Order Service Flow](#1-order-service-flow)
2. [Payment Service Flow](#2-payment-service-flow)
3. [Inventory Service Flow](#3-inventory-service-flow)
4. [Notification Service Flow](#4-notification-service-flow)
5. [Complete End-to-End Flow](#5-complete-end-to-end-flow)
6. [Error Handling Flows](#6-error-handling-flows)

---

## 1. Order Service Flow

### 1.1 Order Producer → Order Created Topic

```mermaid
flowchart TB
    subgraph "Order Producer Layer"
        A[OrderController] -->|POST /api/orders| B[OrderService]
        B -->|createOrder| C[OrderProducer]
    end

    subgraph "Kafka Broker"
        C -->|publish| D[(order-created<br/>topic)]
        D -->|partitions: 3| D
    end

    subgraph "Message Structure"
        E[Order Event]
        E1[orderId: String]
        E2[customerId: String]
        E3[totalAmount: Decimal]
        E4[status: PENDING]
        E5[correlationId: String]
        E6[idempotencyKey: String]
        E --- E1 & E2 & E3 & E4 & E5 & E6
    end

    subgraph "Headers"
        H[correlationId: UTF-8]
    end

    C -->|add headers| H
    H --> D

    style A fill:#90EE90
    style B fill:#87CEEB
    style C fill:#FFB6C1
    style D fill:#DDA0DD
    style E fill:#FFE4B5
    style H fill:#FFE4B5
```

### 1.2 Order Consumer - Processing Order Created Events

```mermaid
flowchart TD
    A[(order-created<br/>topic)] -->|consume| B[OrderConsumer<br/>processOrderCreated]
    
    B --> C{Duplicate Check<br/>idempotencyKey}
    C -->|Yes| D[Log Warning]
    C -->|No| E[OrderService.processOrder]
    
    D --> F[Acknowledge]
    E --> G{Order Status?}
    
    G -->|CANCELLED| H[Add to processedKeys]
    G -->|VALID| I[PaymentService.processPayment]
    
    H --> F
    I --> J[Add to processedKeys]
    J --> F
    
    F --> K[Message Committed]
    
    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#87CEEB
    style G fill:#FFD700
    style I fill:#87CEEB
    style F fill:#98FB98
    style K fill:#90EE90
```

### 1.3 Order Consumer - Multiple Event Handlers

```mermaid
flowchart TB
    subgraph "Order Topics"
        T1[(order-created)]
        T2[(order-confirmed)]
        T3[(order-cancelled)]
        T4[(order-failed)]
    end

    subgraph "OrderConsumer"
        H1[@KafkaListener<br/>order-processor-group]
        H2[@KafkaListener<br/>order-notification-group]
        H3[@KafkaListener<br/>order-cancellation-group]
        H4[@KafkaListener<br/>order-failure-group]
    end

    T1 --> H1
    T2 --> H2
    T3 --> H3
    T4 --> H4

    H1 --> P1[Process Order + Payment]
    H2 --> P2[Log for Notification]
    H3 --> P3[Acknowledge Cancellation]
    H4 --> P4[Acknowledge Failure]

    style T1 fill:#DDA0DD
    style T2 fill:#DDA0DD
    style T3 fill:#DDA0DD
    style T4 fill:#DDA0DD
    style H1 fill:#FFE4B5
    style H2 fill:#FFE4B5
    style H3 fill:#FFE4B5
    style H4 fill:#FFE4B5
    style P1 fill:#87CEEB
    style P2 fill:#87CEEB
    style P3 fill:#87CEEB
    style P4 fill:#87CEEB
```

---

## 2. Payment Service Flow

### 2.1 Payment Producer → Payment Events

```mermaid
flowchart TB
    subgraph "Payment Producer Layer"
        A[PaymentService] -->|processPayment| B[PaymentProducer]
    end

    subgraph "Kafka Broker"
        B -->|publish| C[(payment-processed<br/>topic)]
        B -->|publish| D[(payment-failed<br/>topic)]
    end

    subgraph "Payment Processed Event"
        E[Payment Event]
        E1[paymentId: String]
        E2[orderId: String]
        E3[amount: Decimal]
        E4[status: COMPLETED/FAILED]
        E5[correlationId: String]
        E --- E1 & E2 & E3 & E4 & E5
    end

    B --> E

    C -->|partitions: 3| C
    D -->|partitions: 3| D

    style A fill:#87CEEB
    style B fill:#FFB6C1
    style C fill:#DDA0DD
    style D fill:#DDA0DD
    style E fill:#FFE4B5
```

### 2.2 Payment Consumer - Payment Processed Flow

```mermaid
flowchart TD
    A[(payment-processed<br/>topic)] -->|consume| B[PaymentConsumer<br/>listenPaymentProcessed]
    
    B --> C{Duplicate Check<br/>paymentId}
    C -->|Yes| D[Log Warning]
    C -->|No| E{Payment Status?}
    
    E -->|COMPLETED| F[Build Order from Payment]
    E -->|FAILED| G[Log Error]
    
    D --> H[Acknowledge]
    G --> H
    
    F --> I[OrderEventPublisher<br/>publishInventoryReservationRequest]
    I --> J[(inventory-reservation<br/>topic)]
    
    I --> K[Add to processedKeys]
    K --> L[Acknowledge]
    L --> M[Message Committed]
    
    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#FFD700
    style F fill:#87CEEB
    style I fill:#FFB6C1
    style J fill:#DDA0DD
    style L fill:#98FB98
    style M fill:#90EE90
```

### 2.3 Payment Consumer - Payment Failed Flow

```mermaid
flowchart TD
    A[(payment-failed<br/>topic)] -->|consume| B[PaymentConsumer<br/>listenPaymentFailed]
    
    B --> C{Duplicate Check<br/>paymentId}
    C -->|Yes| D[Log Warning]
    C -->|No| E[Build Order from Payment]
    
    D --> F[Acknowledge]
    
    E --> G[OrderEventPublisher<br/>publishOrderFailed]
    G --> H[(order-failed<br/>topic)]
    
    G --> I[Add to processedKeys]
    I --> F
    F --> J[Message Committed]
    
    subgraph "DLT Handler"
        K[@DltHandler<br/>handleDlt]
        L[Log Payment Details]
        M[Alert Operations]
        K --> L --> M
    end

    H -.->|on failure| K

    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#87CEEB
    style G fill:#FFB6C1
    style H fill:#DDA0DD
    style F fill:#98FB98
    style J fill:#90EE90
    style K fill:#FF6B6B
```

---

## 3. Inventory Service Flow

### 3.1 Inventory Producer → Inventory Events

```mermaid
flowchart TB
    subgraph "Inventory Producer Layer"
        A[InventoryService] -->|reserveInventory| B[InventoryProducer]
    end

    subgraph "Kafka Broker"
        B -->|publish| C[(inventory-reserved<br/>topic)]
        B -->|publish| D[(inventory-released<br/>topic)]
    end

    subgraph "Inventory Reserved Event"
        E[Inventory Event]
        E1[reservationId: String]
        E2[orderId: String]
        E3[items: List]
        E4[status: RESERVED/RELEASED]
        E5[correlationId: String]
        E6[failureReason: String]
        E --- E1 & E2 & E3 & E4 & E5 & E6
    end

    B --> E

    C -->|partitions: 3| C
    D -->|partitions: 3| D

    style A fill:#87CEEB
    style B fill:#FFB6C1
    style C fill:#DDA0DD
    style D fill:#DDA0DD
    style E fill:#FFE4B5
```

### 3.2 Inventory Consumer - Reservation Request Flow

```mermaid
flowchart TD
    A[(inventory-reservation<br/>topic)] -->|consume| B[InventoryConsumer<br/>listenInventoryReservation]
    
    B --> C{Duplicate Check<br/>idempotencyKey}
    C -->|Yes| D[Log Warning]
    C -->|No| E[InventoryService<br/>reserveInventoryAndPublish]
    
    D --> F[Acknowledge]
    
    E --> G{Reservation Success?}
    G -->|Yes| H[InventoryProducer<br/>publishInventoryReserved]
    G -->|No| I[InventoryProducer<br/>publishInventoryReleased]
    
    H --> J[(inventory-reserved<br/>topic)]
    I --> K[(inventory-released<br/>topic)]
    
    E --> L[Add to processedKeys]
    L --> M[Acknowledge]
    M --> N[Message Committed]
    
    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#87CEEB
    style H fill:#FFB6C1
    style I fill:#FFB6C1
    style J fill:#DDA0DD
    style K fill:#DDA0DD
    style M fill:#98FB98
    style N fill:#90EE90
```

### 3.3 Inventory Consumer - Inventory Reserved Flow

```mermaid
flowchart TD
    A[(inventory-reserved<br/>topic)] -->|consume| B[InventoryConsumer<br/>processInventoryReserved]
    
    B --> C{Duplicate Check<br/>reservationId}
    C -->|Yes| D[Log Warning]
    C -->|No| E[Build Order from Inventory]
    
    D --> F[Acknowledge]
    
    E --> G[OrderService<br/>confirmOrder]
    G --> H[NotificationService<br/>sendOrderConfirmation]
    H --> I[NotificationEventPublisher<br/>publishEmailNotification]
    
    I --> J[(notification-email<br/>topic)]
    
    I --> K[Add to processedKeys]
    K --> L[Acknowledge]
    L --> M[Message Committed]
    
    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#87CEEB
    style G fill:#87CEEB
    style H fill:#87CEEB
    style I fill:#FFB6C1
    style J fill:#DDA0DD
    style L fill:#98FB98
    style M fill:#90EE90
```

### 3.4 Inventory Consumer - Inventory Released Flow

```mermaid
flowchart TD
    A[(inventory-released<br/>topic)] -->|consume| B[InventoryConsumer<br/>processInventoryReleased]
    
    B --> C{Duplicate Check<br/>reservationId}
    C -->|Yes| D[Log Warning]
    C -->|No| E[Build Order from Inventory]
    
    D --> F[Acknowledge]
    
    E --> G[OrderService<br/>failOrder<br/>with failureReason]
    G --> H[(order-failed<br/>topic)]
    
    G --> I[Add to processedKeys]
    I --> L[Acknowledge]
    L --> M[Message Committed]
    
    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#87CEEB
    style G fill:#87CEEB
    style H fill:#DDA0DD
    style L fill:#98FB98
    style M fill:#90EE90
```

---

## 4. Notification Service Flow

### 4.1 Notification Producer → Notification Events

```mermaid
flowchart TB
    subgraph "Notification Producer Layer"
        A[NotificationService] -->|sendNotification| B[NotificationProducer]
    end

    subgraph "Kafka Broker"
        B -->|publish| C[(notification-email<br/>topic)]
        B -->|publish| D[(notification-sms<br/>topic)]
    end

    subgraph "Notification Event"
        E[Notification Event]
        E1[notificationId: String]
        E2[orderId: String]
        E3[recipient: String]
        E4[type: EMAIL/SMS]
        E5[subject: String]
        E6[content: String]
        E --- E1 & E2 & E3 & E4 & E5 & E6
    end

    B --> E

    C -->|partitions: 3| C
    D -->|partitions: 3| D

    style A fill:#87CEEB
    style B fill:#FFB6C1
    style C fill:#DDA0DD
    style D fill:#DDA0DD
    style E fill:#FFE4B5
```

### 4.2 Notification Consumer - Email Notification Flow

```mermaid
flowchart TD
    A[(notification-email<br/>topic)] -->|consume| B[NotificationConsumer<br/>listenEmailNotification]
    
    B --> C{Duplicate Check<br/>notificationId}
    C -->|Yes| D[Log Warning]
    C -->|No| E[Log Notification Details]
    
    D --> F[Acknowledge]
    
    E --> G[Simulate Send Email<br/>Thread.sleep 100ms]
    G --> H{Success?}
    
    H -->|Yes| I[Log Success]
    H -->|No| J[Throw Exception]
    
    I --> K[Add to processedNotifications]
    K --> L[Acknowledge]
    L --> M[Message Committed]
    
    J --> N[Retry via Spring Kafka]
    N -->|after retries| O[(notification-email-dlt<br/>topic)]
    
    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#87CEEB
    style G fill:#98FB98
    style I fill:#90EE90
    style L fill:#98FB98
    style M fill:#90EE90
    style O fill:#FF6B6B
```

### 4.3 Notification Consumer - SMS Notification Flow

```mermaid
flowchart TD
    A[(notification-sms<br/>topic)] -->|consume| B[NotificationConsumer<br/>listenSMSNotification]
    
    B --> C{Duplicate Check<br/>notificationId}
    C -->|Yes| D[Log Warning]
    C -->|No| E[Log Notification Details]
    
    D --> F[Acknowledge]
    
    E --> G[Simulate Send SMS<br/>Thread.sleep 50ms]
    G --> H{Success?}
    
    H -->|Yes| I[Log Success]
    H -->|No| J[Throw Exception]
    
    I --> K[Add to processedNotifications]
    K --> L[Acknowledge]
    L --> M[Message Committed]
    
    J --> N[Retry via Spring Kafka]
    N -->|after retries| O[(notification-sms-dlt<br/>topic)]
    
    style A fill:#DDA0DD
    style B fill:#FFE4B5
    style C fill:#FFD700
    style E fill:#87CEEB
    style G fill:#98FB98
    style I fill:#90EE90
    style L fill:#98FB98
    style M fill:#90EE90
    style O fill:#FF6B6B
```

---

## 5. Complete End-to-End Flow

### 5.1 Happy Path - Order Success Flow

```mermaid
sequenceDiagram
    participant Client
    participant OrderCtrl as OrderController
    participant OrderSvc as OrderService
    participant OrderProd as OrderProducer
    participant OrderTopic as order-created
    participant OrderCons as OrderConsumer
    participant PaymentSvc as PaymentService
    participant PaymentProd as PaymentProducer
    participant PaymentTopic as payment-processed
    participant PaymentCons as PaymentConsumer
    participant InventoryTopic as inventory-reservation
    participant InventoryCons as InventoryConsumer
    participant InventorySvc as InventoryService
    participant InventoryProd as InventoryProducer
    participant InventoryReservedTopic as inventory-reserved
    participant InvEventCons as InventoryConsumer
    participant OrderSvc2 as OrderService
    participant NotifSvc as NotificationService
    participant NotifProd as NotificationProducer
    participant NotifTopic as notification-email
    participant NotifCons as NotificationConsumer

    Client->>OrderCtrl: POST /api/orders
    OrderCtrl->>OrderSvc: createOrder
    OrderSvc->>OrderProd: publishOrderCreated
    OrderProd->>OrderTopic: publish event

    OrderTopic->>OrderCons: consume
    OrderCons->>OrderSvc: processOrder
    OrderCons->>PaymentSvc: processPayment
    PaymentSvc->>PaymentProd: publishPaymentProcessed
    PaymentProd->>PaymentTopic: publish event

    PaymentTopic->>PaymentCons: consume
    PaymentCons->>OrderProd: publishInventoryReservationRequest
    OrderProd->>InventoryTopic: publish event

    InventoryTopic->>InventoryCons: consume
    InventoryCons->>InventorySvc: reserveInventoryAndPublish
    InventorySvc->>InventoryProd: publishInventoryReserved
    InventoryProd->>InventoryReservedTopic: publish event

    InventoryReservedTopic->>InvEventCons: consume
    InvEventCons->>OrderSvc2: confirmOrder
    InvEventCons->>NotifSvc: sendOrderConfirmation
    NotifSvc->>NotifProd: publishEmailNotification
    NotifProd->>NotifTopic: publish event

    NotifTopic->>NotifCons: consume
    NotifCons->>NotifCons: Send email

    Note over Client,NotifCons: ✅ Order Complete!
```

### 5.2 Component Architecture Overview

```mermaid
graph TB
    subgraph "Controller Layer 🟢"
        OrderCtrl[OrderController]
    end

    subgraph "Service Layer 🔵"
        OrderSvc[OrderService]
        PaymentSvc[PaymentService]
        InventorySvc[InventoryService]
        NotificationSvc[NotificationService]
    end

    subgraph "Producer Layer 🔴"
        OrderProd[OrderProducer]
        PaymentProd[PaymentProducer]
        InventoryProd[InventoryProducer]
        NotificationProd[NotificationProducer]
    end

    subgraph "Kafka Topics 🟣"
        OrderCreated[(order-created)]
        OrderConfirmed[(order-confirmed)]
        OrderFailed[(order-failed)]
        PaymentProcessed[(payment-processed)]
        PaymentFailed[(payment-failed)]
        InventoryReservation[(inventory-reservation)]
        InventoryReserved[(inventory-reserved)]
        InventoryReleased[(inventory-released)]
        NotificationEmail[(notification-email)]
        NotificationSMS[(notification-sms)]
    end

    subgraph "Consumer Layer 🟠"
        OrderCons[OrderConsumer]
        PaymentCons[PaymentConsumer]
        InventoryCons[InventoryConsumer]
        NotificationCons[NotificationConsumer]
    end

    OrderCtrl --> OrderSvc
    OrderSvc --> OrderProd
    OrderProd --> OrderCreated
    OrderCreated --> OrderCons
    OrderCons --> PaymentSvc
    PaymentSvc --> PaymentProd
    PaymentProd --> PaymentProcessed
    PaymentProcessed --> PaymentCons
    PaymentCons --> OrderProd
    OrderProd --> InventoryReservation
    InventoryReservation --> InventoryCons
    InventoryCons --> InventorySvc
    InventorySvc --> InventoryProd
    InventoryProd --> InventoryReserved
    InventoryProd --> InventoryReleased
    InventoryReserved --> InventoryCons
    InventoryReleased --> InventoryCons
    InventoryCons --> OrderSvc
    InventoryCons --> NotificationSvc
    NotificationSvc --> NotificationProd
    NotificationProd --> NotificationEmail
    NotificationProd --> NotificationSMS
    NotificationEmail --> NotificationCons
    NotificationSMS --> NotificationCons

    style OrderCtrl fill:#90EE90
    style OrderSvc fill:#87CEEB
    style PaymentSvc fill:#87CEEB
    style InventorySvc fill:#87CEEB
    style NotificationSvc fill:#87CEEB
    style OrderProd fill:#FFB6C1
    style PaymentProd fill:#FFB6C1
    style InventoryProd fill:#FFB6C1
    style NotificationProd fill:#FFB6C1
    style OrderCreated fill:#DDA0DD
    style OrderConfirmed fill:#DDA0DD
    style OrderFailed fill:#DDA0DD
    style PaymentProcessed fill:#DDA0DD
    style PaymentFailed fill:#DDA0DD
    style InventoryReservation fill:#DDA0DD
    style InventoryReserved fill:#DDA0DD
    style InventoryReleased fill:#DDA0DD
    style NotificationEmail fill:#DDA0DD
    style NotificationSMS fill:#DDA0DD
    style OrderCons fill:#FFE4B5
    style PaymentCons fill:#FFE4B5
    style InventoryCons fill:#FFE4B5
    style NotificationCons fill:#FFE4B5
```

---

## 6. Error Handling Flows

### 6.1 Error Flow - Payment Failure

```mermaid
flowchart TD
    A[OrderConsumer<br/>processOrderCreated] -->|processPayment| B[PaymentService]
    
    B --> C{Payment Success?}
    C -->|Yes| D[Continue Flow]
    C -->|No| E[PaymentProducer<br/>publishPaymentFailed]
    
    E --> F[(payment-failed<br/>topic)]
    F -->|consume| G[PaymentConsumer<br/>listenPaymentFailed]
    
    G --> H[OrderEventPublisher<br/>publishOrderFailed]
    H --> I[(order-failed<br/>topic)]
    
    I -->|consume| J[OrderConsumer<br/>processOrderFailed]
    J --> K[Acknowledge & Log]
    
    subgraph "Compensation Actions"
        L[Release Reserved Items]
        M[Update Order Status: FAILED]
        N[Send Failure Notification]
    end

    I -.-> L
    I -.-> M
    I -.-> N

    style A fill:#FFE4B5
    style B fill:#87CEEB
    style C fill:#FFD700
    style E fill:#FFB6C1
    style F fill:#DDA0DD
    style G fill:#FFE4B5
    style H fill:#FFB6C1
    style I fill:#DDA0DD
    style J fill:#FFE4B5
    style K fill:#98FB98
    style L fill:#FF6B6B
    style M fill:#FF6B6B
    style N fill:#FF6B6B
```

### 6.2 Error Flow - Inventory Reservation Failure

```mermaid
flowchart TD
    A[InventoryConsumer<br/>listenInventoryReservation] -->|reserveInventory| B[InventoryService]
    
    B --> C{Inventory Available?}
    C -->|Yes| D[Continue Flow]
    C -->|No| E[InventoryProducer<br/>publishInventoryReleased]
    
    E --> F[(inventory-released<br/>topic)]
    F -->|consume| G[InventoryConsumer<br/>processInventoryReleased]
    
    G --> H[OrderService<br/>failOrder]
    H --> I[OrderProducer<br/>publishOrderFailed]
    
    I --> J[(order-failed<br/>topic)]
    
    subgraph "Failure Reason Tracking"
        K[failureReason: OUT_OF_STOCK]
        L[failureReason: RESERVATION_TIMEOUT]
        M[failureReason: INVALID_ITEM]
    end

    E -.-> K
    E -.-> L
    E -.-> M

    style A fill:#FFE4B5
    style B fill:#87CEEB
    style C fill:#FFD700
    style E fill:#FFB6C1
    style F fill:#DDA0DD
    style G fill:#FFE4B5
    style H fill:#87CEEB
    style I fill:#FFB6C1
    style J fill:#DDA0DD
    style K fill:#FF6B6B
    style L fill:#FF6B6B
    style M fill:#FF6B6B
```

### 6.3 Error Flow - Dead Letter Topic Handling

```mermaid
flowchart TD
    A[Consumer receives message] --> B{Processing Success?}
    
    B -->|Yes| C[Acknowledge]
    B -->|No| D[Retry Attempt 1]
    
    D --> E{Success?}
    E -->|Yes| C
    E -->|No| F[Retry Attempt 2]
    
    F --> G{Success?}
    G -->|Yes| C
    G -->|No| H[Send to DLT]
    
    H --> I[(topic-dlt<br/>topic)]
    
    subgraph "DLT Monitoring"
        J[Alert Operations Team]
        K[Log Error Details]
        L[Manual Intervention Required]
        M[Replay Messages if needed]
    end

    I --> J
    I --> K
    I --> L
    I --> M

    style A fill:#FFE4B5
    style B fill:#FFD700
    style C fill:#90EE90
    style D fill:#FFA500
    style E fill:#FFD700
    style F fill:#FFA500
    style G fill:#FFD700
    style H fill:#FF6B6B
    style I fill:#DC143C
    style J fill:#FF6B6B
    style K fill:#FF6B6B
    style L fill:#FF6B6B
    style M fill:#FFA500
```

### 6.4 Retry Configuration Flow

```mermaid
flowchart LR
    subgraph "Retry Configuration"
        A[DefaultErrorHandler]
        B[DefaultBackOff<br/>initialInterval: 1s<br/>multiplier: 2.0<br/>maxInterval: 10s<br/>maxAttempts: 3]
        A --> B
    end

    subgraph "Retry Topics"
        C[topic]
        D[topic.RETRY.0]
        E[topic.RETRY.1]
        F[topic.DLT]
    end

    C -->|failure| D
    D -->|failure| E
    E -->|failure| F

    style A fill:#FFE4B5
    style B fill:#FFE4B5
    style C fill:#DDA0DD
    style D fill:#FFA500
    style E fill:#FFA500
    style F fill:#DC143C
```

---

## 7. Topic Summary Table

| Topic | Partitions | Replicas | Producer | Consumer Group(s) |
|-------|-----------|----------|----------|-------------------|
| `order-created` | 3 | 1 | OrderProducer | order-processor-group |
| `order-confirmed` | 3 | 1 | OrderProducer | order-notification-group |
| `order-cancelled` | 3 | 1 | OrderProducer | order-cancellation-group |
| `order-failed` | 3 | 1 | OrderProducer | order-failure-group |
| `payment-processed` | 3 | 1 | PaymentProducer | payment-confirmation-group |
| `payment-failed` | 3 | 1 | PaymentProducer | payment-failure-group |
| `inventory-reservation` | 3 | 1 | OrderProducer | inventory-reservation-group |
| `inventory-reserved` | 3 | 1 | InventoryProducer | order-confirmation-group |
| `inventory-released` | 3 | 1 | InventoryProducer | inventory-failure-group |
| `notification-email` | 3 | 1 | NotificationProducer | notification-email-group |
| `notification-sms` | 3 | 1 | NotificationProducer | notification-sms-group |

---

## 8. Legend

| Color | Component Type |
|-------|---------------|
| 🟢 Green | Controller Layer (REST endpoints) |
| 🔵 Blue | Service Layer (Business logic) |
| 🔴 Red | Producer Layer (Event publishing) |
| 🟣 Purple | Kafka Topics |
| 🟠 Orange | Consumer Layer (Event handling) |
| 🟡 Yellow | Decision Points / Conditions |
| 🟢 Light Green | Success / Acknowledge |
| 🔴 Light Red | Error / Failure / DLT |

---

Happy Building! 🚀
