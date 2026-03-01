package com.learning.kafka;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class KafkaMasteryApplication {

    @Value("${kafka.consumer.mode:standard}")
    private String consumerMode;

    public static void main(String[] args) {
        SpringApplication.run(KafkaMasteryApplication.class, args);
    }

    @PostConstruct
    public void printStartupMessage() {
        String modeDisplay = "saga".equalsIgnoreCase(consumerMode) 
                ? "🔄 SAGA ORCHESTRATOR MODE" 
                : "⚡ STANDARD CONSUMER MODE";
        
        System.out.println("""

                ╔═══════════════════════════════════════════════════════════╗
                ║               Project Started Successfully                ║
                ╠═══════════════════════════════════════════════════════════╣
                ║  Kafka UI: http://localhost:8090                          ║
                ║  Metrics: http://localhost:8082/actuator/prometheus       ║
                ║  📚 Open README.md to start your learning journey!        ║
                ║                                                           ║
                ║  Active Mode: %s
                ║  Outbox Publisher: Running (5s interval)                  ║
                ║  @KafkaListener: Active and waiting for messages          ║
                ╚═══════════════════════════════════════════════════════════╝

                """.formatted(modeDisplay));
        
        if ("saga".equalsIgnoreCase(consumerMode)) {
            log.info("Saga Orchestrator Mode enabled - OrderSagaOrchestrator is ACTIVE");
            log.info("Standard consumers (OrderConsumer, PaymentConsumer, InventoryConsumer, NotificationConsumer) are DISABLED");
        } else {
            log.info("Standard Consumer Mode enabled - Standard consumers are ACTIVE");
            log.info("OrderSagaOrchestrator is DISABLED");
        }
    }
}
