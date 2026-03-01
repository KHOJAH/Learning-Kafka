package com.learning.kafka.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.learning.kafka.exception.NonRetryableException;
import com.learning.kafka.exception.RetryableException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.net.ConnectException;
import java.sql.SQLTransientConnectionException;
import java.util.concurrent.TimeoutException;

/**
 * Key Concepts:
 * - DefaultErrorHandler: Handles exceptions during message processing
 * - DeadLetterPublishingRecoverer: Sends failed messages to DLT
 * - FixedBackOff: Waits between retry attempts
 * - Retry vs Non-Retry exceptions: Classify which errors should be retried
 */
@Configuration
public class KafkaErrorHandlingConfig {

    /**
     * Default Error Handler with Dead Letter Topic
     *
     * This error handler:
     * 1. Catches exceptions during message processing
     * 2. Retries up to 3 times with 1 second delay
     * 3. Sends to Dead Letter Topic after all retries exhausted
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaOperations<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        errorHandler.addRetryableExceptions(
            ConnectException.class,
            SQLTransientConnectionException.class,
            TimeoutException.class
        );
        
        errorHandler.addNotRetryableExceptions(
            NonRetryableException.class,
            JsonMappingException.class,
            IllegalArgumentException.class,
            RetryableException.class
        );

        return errorHandler;
    }

    /**
     * Exponential Backoff Error Handler
     * 
     * Provides increasing delays between retry attempts:
     * - Attempt 1: 1 second delay
     * - Attempt 2: 2 second delay
     * - Attempt 3: 4 second delay (capped at maxInterval)
     * 
     * This prevents overwhelming failing services and gives them time to recover.
     */
    @Bean
    public DefaultErrorHandler exponentialBackoffErrorHandler(KafkaOperations<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);
        backOff.setMaxAttempts(3);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addRetryableExceptions(
            ConnectException.class,
            SQLTransientConnectionException.class,
            TimeoutException.class
        );
        errorHandler.addNotRetryableExceptions(
            NonRetryableException.class,
            JsonMappingException.class,
            IllegalArgumentException.class
        );
        
        return errorHandler;
    }

    /**
     * Custom Error Handler for Notification Consumer
     * 
     * Notifications are less time-critical than payments, so:
     * - Longer initial delay (5 seconds instead of 1)
     * - More retry attempts (5 instead of 3)
     * - Gives external notification services time to recover
     */
    @Bean
    public DefaultErrorHandler notificationErrorHandler(KafkaOperations<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(5000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(30000L);
        backOff.setMaxAttempts(5);
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addRetryableExceptions(
            ConnectException.class,
            TimeoutException.class
        );
        errorHandler.addNotRetryableExceptions(
            NonRetryableException.class,
            JsonMappingException.class,
            IllegalArgumentException.class
        );
        
        return errorHandler;
    }

}
