package com.learning.kafka.saga;

import com.learning.kafka.model.Inventory;
import com.learning.kafka.model.Order;
import com.learning.kafka.model.Payment;
import com.learning.kafka.producer.InventoryProducer;
import com.learning.kafka.producer.PaymentProducer;
import com.learning.kafka.repository.SagaStateRepository;
import com.learning.kafka.service.InventoryService;
import com.learning.kafka.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.learning.kafka.model.Inventory.ReservationStatus.RESERVED;
import static com.learning.kafka.model.Payment.PaymentStatus.COMPLETED;
import static com.learning.kafka.saga.SagaState.SagaStatus.IN_PROGRESS;
import static com.learning.kafka.saga.SagaState.SagaStep.PAYMENT_PROCESSED;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final PaymentProducer paymentProducer;
    private final InventoryProducer inventoryProducer;
    private final SagaStateRepository sagaStateRepository;

    private static final long SAGA_TIMEOUT_MINUTES = 5;

    @KafkaListener(topics = "order-created", groupId = "saga-orchestrator-group")
    public void processOrder(Order order) {
        log.info("Saga Step 1: Processing payment for order: {}", order.getOrderId());

        try {
            Payment payment = paymentService.processPayment(order);

            if (COMPLETED.equals(payment.getStatus())) {
                saveSagaState(order.getOrderId(), order.getCorrelationId(),
                        PAYMENT_PROCESSED, IN_PROGRESS);
                paymentProducer.sendPaymentProcessed(payment);
                log.info("Saga Step 1 Complete: Payment processed");
            }
        } catch (Exception e) {
            log.error("Saga Step 1 Failed: Payment failed", e);

            compensatePayment(order, e.getMessage());
        }
    }


    @KafkaListener(topics = "payment-processed", groupId = "saga-orchestrator-group")
    public void reserveInventory(Payment payment) {
        log.info("Saga Step 2: Reserving inventory for order: {}", payment.getOrderId());

        Order order = new Order();
        order.setOrderId(payment.getOrderId());
        order.setCorrelationId(payment.getCorrelationId());
        order.setTotalAmount(payment.getAmount());
        order.setItems("SKU-" + payment.getOrderId());

        Inventory inventory = Inventory.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getItems(),
                1,
                "WAREHOUSE-001"
        );
        try {
            Inventory reservedInventory = inventoryService.reserveInventory(inventory);

            if (RESERVED.equals(reservedInventory.getStatus())) {
                saveSagaState(order.getOrderId(), order.getCorrelationId(),
                        SagaState.SagaStep.INVENTORY_RESERVED, IN_PROGRESS);
                inventoryProducer.sendInventoryReserved(reservedInventory);
                log.info("Saga Step 2 Complete: Inventory reserved");
            }
        } catch (Exception e) {
            log.error("Saga Step 2 Failed: Inventory reservation failed", e);
            compensatePayment(order, e.getMessage());
            Inventory releaseInventory = inventoryService.releaseInventory(order);
            inventoryProducer.publishInventoryReleased(releaseInventory);
        }
    }


    private void compensatePayment(Order order, String reason) {
        log.info("Compensating payment for order: {}", order.getOrderId());

        Payment refund = Payment.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getTotalAmount(),
                Payment.PaymentMethod.CREDIT_CARD
        ).fail("Refund: " + reason);

        saveSagaState(order.getOrderId(), order.getCorrelationId(),
                SagaState.SagaStep.COMPENSATING, SagaState.SagaStatus.FAILED);

        paymentProducer.sendPaymentFailed(refund);
    }


    @Scheduled(fixedRate = 60000)
    public void checkSagaTimeouts() {
        log.debug("Checking for timed-out sagas");

        List<SagaState> timedOutSagas = sagaStateRepository
                .findByStatusAndUpdatedAtBefore(IN_PROGRESS,
                        Instant.now().minus(Duration.ofMinutes(SAGA_TIMEOUT_MINUTES)));

        for (SagaState saga : timedOutSagas) {
            log.warn("Saga timed out: sagaId={}, orderId={}, currentStep={}",
                    saga.getSagaId(), saga.getOrderId(), saga.getCurrentStep());

            handleSagaTimeout(saga);
        }
    }

    private void handleSagaTimeout(SagaState saga) {
        Order order = new Order();
        order.setOrderId(saga.getOrderId());
        order.setCorrelationId(saga.getCorrelationId());

        switch (saga.getCurrentStep()) {
            case PAYMENT_PROCESSED:
                log.info("Timeout: Payment processed but inventory not reserved. Compensating payment.");
                compensatePayment(order, "Saga timeout: Inventory reservation not completed within "
                        + SAGA_TIMEOUT_MINUTES + " minutes");
                break;
            case INVENTORY_RESERVED:
                log.info("Timeout: Inventory reserved but order not completed. Releasing inventory.");
                compensateInventory(order, "Saga timeout: Order completion not completed within "
                        + SAGA_TIMEOUT_MINUTES + " minutes");
                break;
            default:
                log.warn("Unknown saga step for timeout handling: {}", saga.getCurrentStep());
        }
    }

    private void compensateInventory(Order order, String reason) {
        log.info("Compensating inventory for order: {}", order.getOrderId());

        Inventory inventory = Inventory.create(
                order.getOrderId(),
                order.getCorrelationId(),
                order.getItems() != null ? order.getItems() : "SKU-" + order.getOrderId(),
                1,
                "WAREHOUSE-001"
        );

        inventoryService.releaseInventory(order);
        inventoryProducer.sendInventoryReleased(inventory);

        saveSagaState(order.getOrderId(), order.getCorrelationId(),
                SagaState.SagaStep.COMPENSATING, SagaState.SagaStatus.FAILED);
    }


    private void saveSagaState(String orderId, String correlationId,
                               SagaState.SagaStep step, SagaState.SagaStatus status) {
        SagaState sagaState = sagaStateRepository.findByOrderId(orderId)
                .orElse(new SagaState());

        sagaState.setSagaId(correlationId);
        sagaState.setOrderId(orderId);
        sagaState.setCorrelationId(correlationId);
        sagaState.setCurrentStep(step);
        sagaState.setStatus(status);
        sagaState.setUpdatedAt(Instant.now());

        if (sagaState.getCreatedAt() == null) {
            sagaState.setCreatedAt(Instant.now());
        }

        sagaStateRepository.save(sagaState);
        log.debug("Saga state saved: orderId={}, step={}, status={}", orderId, step, status);
    }

}
