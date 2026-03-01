package com.learning.kafka.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.List;

/**
 * Key Metrics to Monitor:
 * - Producer: record-send-rate, request-latency-avg, compression-rate
 * - Consumer: records-consumed-rate, fetch-latency, commit-latency
 * - Consumer Lag: difference between latest offset and current position
 */
@Configuration
public class KafkaMonitoringConfig {

    @Bean
    public KafkaClientMetrics producerMetrics(MeterRegistry meterRegistry,
                                              ProducerFactory<String, Object> producerFactory) {
        Producer<String, Object> producer = producerFactory.createProducer();
        KafkaClientMetrics metrics = new KafkaClientMetrics(producer, List.of(Tag.of("client", "producer")));
        metrics.bindTo(meterRegistry);
        return metrics;
    }

    // CHALLENGE 6.4: Configure consumer metrics
    // - Create Consumer instance from ConsumerFactory
    // - Bind consumer metrics to Micrometer registry
    // - Tag metrics with "consumer" identifier
    @Bean
    public KafkaClientMetrics consumerMetrics(MeterRegistry meterRegistry,
                                              ConsumerFactory<String, Object> consumerFactory) {
        Consumer<String, Object> consumer = consumerFactory.createConsumer();
        KafkaClientMetrics metrics = new KafkaClientMetrics(consumer, List.of(Tag.of("client", "consumer")));
        metrics.bindTo(meterRegistry);
        return metrics;
    }

    // CHALLENGE 6.7: Understand Prometheus integration
    // - Add micrometer-registry-prometheus dependency
    // - Enable /actuator/prometheus endpoint in application.yml
    // - Configure Prometheus server to scrape the endpoint
    // - Metrics are pulled automatically by Prometheus
}
