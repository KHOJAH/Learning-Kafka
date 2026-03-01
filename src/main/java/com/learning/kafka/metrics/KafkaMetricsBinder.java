package com.learning.kafka.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides three key business metrics:
 * - Gauge: Orders processed count (real-time tracking)
 * - Counter: Payment failure events (cumulative count)
 * - Timer: Message processing duration (latency measurement)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMetricsBinder {

    private final MeterRegistry meterRegistry;

    // Atomic variables for thread-safe metric tracking
    private final AtomicInteger ordersProcessedCount = new AtomicInteger(0);
    private final AtomicLong paymentFailureCount = new AtomicLong(0);

    // Timer for message processing duration
    private Timer messageProcessingTimer;

    // Counters and Gauges
    private Gauge ordersProcessedGauge;
    private Counter paymentFailureCounter;

    @PostConstruct
    public void init() {
        log.info("Initializing Kafka business metrics binder");
        registerMetrics();
    }

    private void registerMetrics() {
        ordersProcessedGauge = Gauge.builder(
                        "kafka.orders.processed.count",
                        ordersProcessedCount,
                        AtomicInteger::get
                )
                .description("Total number of orders processed through Kafka")
                .tag("type", "order")
                .tag("application", "kafka-mastery")
                .register(meterRegistry);

        paymentFailureCounter = Counter.builder(
                        "kafka.payment.failures.total"
                )
                .description("Total number of payment failure events")
                .tag("type", "payment")
                .tag("application", "kafka-mastery")
                .tag("severity", "error")
                .register(meterRegistry);

        messageProcessingTimer = Timer.builder(
                        "kafka.message.processing.duration"
                )
                .description("Duration of message processing in Kafka consumers")
                .tag("application", "kafka-mastery")
                .sla(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(5))
                .register(meterRegistry);

        log.info("Kafka business metrics registered successfully");
        log.info("  - Gauge: kafka.orders.processed.count");
        log.info("  - Counter: kafka.payment.failures.total");
        log.info("  - Timer: kafka.message.processing.duration");
    }

    public void incrementOrdersProcessed() {
        int newValue = ordersProcessedCount.incrementAndGet();
        log.debug("Orders processed count incremented to: {}", newValue);
    }

    public void recordPaymentFailure() {
        paymentFailureCounter.increment();
        long currentValue = paymentFailureCount.incrementAndGet();
        log.debug("Payment failure recorded. Total failures: {}", currentValue);
    }


    public Timer.Sample startProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopProcessingTimer(Timer.Sample sample) {
        if (sample != null) {
            sample.stop(messageProcessingTimer);
            log.debug("Message processing duration recorded");
        }
    }


    public int getOrdersProcessedCount() {
        return ordersProcessedCount.get();
    }

    public long getPaymentFailureCount() {
        return paymentFailureCount.get();
    }

}
