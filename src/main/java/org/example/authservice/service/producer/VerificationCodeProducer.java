package org.example.authservice.service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.dto.event.SendCodeEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeProducer {

    private final KafkaTemplate<String, SendCodeEvent> kafkaTemplate;

    @Value("${topics.verification-codes}")
    private String topic;

    public void publish(String email, String code) {
        SendCodeEvent event = new SendCodeEvent(email, code);
        kafkaTemplate.send(topic, email, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to publish verification code to Kafka. email={}, {}", email, exception.getMessage());
                    } else {
                        log.info("Verification code published. topic={}, partition={}, offset={}, email={}",
                                result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), email);
                    }
                });
    }
}