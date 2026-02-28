package com.learning.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration
 *
 * Configures the Kafka consumer with optimal settings for reliability and parallelism.
 *
 * Key Configuration Concepts:
 * - group-id: Identifies the consumer group (for load balancing)
 * - auto-offset-reset=earliest: Start from beginning if no offset exists
 * - enable-auto-commit=false: Manual offset control (more reliable)
 * - isolation.level=read_committed: Only read committed transactional messages
 *
 * CHALLENGE 3.1: Research consumer configurations
 * TODO: Look up what each configuration does in Kafka documentation
 * 💡 Hint: https://kafka.apache.org/documentation/#consumerconfigs
 *
 * @author Kafka Mastery Project
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:kafka-mastery-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;

    private final ObjectMapper objectMapper;

    public KafkaConsumerConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Consumer Factory
     *
     * Creates and configures the consumer factory with all necessary settings.
     *
     * CHALLENGE 3.2: Understand JsonDeserializer
     * TODO: Research how JsonDeserializer converts JSON bytes to Java objects
     * 💡 Hint: It needs trusted packages configuration for security
     *
     * 📝 Solution: JsonDeserializer uses Jackson and needs package trust configuration
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Security - trust all packages (in production, be more specific)
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Transaction support
        // CHALLENGE 3.3: Add isolation level configuration
        // TODO: Set isolation.level to "read_committed"
        // 💡 Hint: Use ConsumerConfig.ISOLATION_LEVEL_CONFIG
        // 📝 SOLUTION: Uncomment and complete the line below
        // configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "___");

        // TODO: Add the configuration above (challenge 3.3)
        // Write your code here




        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(this.objectMapper));
    }

    /**
     * Kafka Listener Container Factory
     *
     * Creates the container factory used by @KafkaListener annotations.
     * This factory configures how consumers are created and managed.
     *
     * CHALLENGE 3.4: Understand ContainerFactory
     * TODO: Research the difference between ConcurrentKafkaListenerContainerFactory
     *       and KafkaListenerContainerFactory
     * 💡 Hint: One supports concurrency, the other doesn't
     *
     * 📝 Solution:
     * - ConcurrentKafkaListenerContainerFactory: Creates multiple consumer instances
     * - KafkaListenerContainerFactory: Single consumer instance
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Manual acknowledgment mode (for manual offset commit)
        // CHALLENGE 3.5: Set acknowledgment mode to MANUAL
        // TODO: Use ContainerProperties.AckMode.MANUAL
        // 💡 Hint: factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        // 📝 SOLUTION: Uncomment the line below
        // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Concurrency configuration
        // CHALLENGE 3.6: Set concurrency level
        // TODO: Set concurrency to 3 (creates 3 consumer instances)
        // 💡 Hint: factory.setConcurrency(3)
        // 📝 SOLUTION: Uncomment the line below
        // factory.setConcurrency(3);

        // TODO: Add the configurations above (challenges 3.5-3.6)
        // Write your code here




        return factory;
    }

    /**
     * CHALLENGE 3.7: Create a high-throughput consumer factory
     * TODO: Create a @Bean method for a separate consumer factory optimized for high throughput
     * TODO: Set concurrency to 6 or higher
     * TODO: Use a different group-id suffix (e.g., "-high-throughput")
     *
     * 💡 Hint: High-throughput consumers process messages faster but may use more resources
     *
     * 📝 SOLUTION (DON'T LOOK YET!):
     * ```java
     * @Bean
     * public ConsumerFactory<String, Object> highThroughputConsumerFactory() {
     *     Map<String, Object> configProps = new HashMap<>();
     *     configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
     *     configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-high-throughput");
     *     configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
     *     configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
     *     configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
     *     configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
     *     configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
     *     return new DefaultKafkaConsumerFactory<>(configProps);
     * }
     *
     * @Bean
     * public ConcurrentKafkaListenerContainerFactory<String, Object> highThroughputListenerFactory() {
     *     ConcurrentKafkaListenerContainerFactory<String, Object> factory =
     *             new ConcurrentKafkaListenerContainerFactory<>();
     *     factory.setConsumerFactory(highThroughputConsumerFactory());
     *     factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
     *     factory.setConcurrency(6); // Higher concurrency
     *     return factory;
     * }
     * ```
     */
    // TODO: Add high-throughput consumer factory (see challenge above)




}
