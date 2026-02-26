package com.learning.kafka.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Order Request DTO - Used for creating orders via REST API
 * <p>
 * This is the request body that clients send to create an order.
 * It gets validated and then converted to an Order model for Kafka publishing.
 *
 * @author Kafka Mastery Project
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;
    @NotBlank(message = "Items are required")
    private String items;
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
}
