package org.example.authservice.service.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.authservice.dto.event.SendCodeEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeProducer {

    private final KafkaProducer<String, String> kafkaProducer;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.verification}")
    private String topic;

    public void publish(String email, String code) {
        try {
            String json = objectMapper.writeValueAsString(new SendCodeEvent(email, code));
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, email, json);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish verification code to Kafka. email={}, {}", email, exception.getMessage());
                } else {
                    log.info("Verification code published. topic={}, partition={}, offset={}, email={}",
                            metadata.topic(), metadata.partition(), metadata.offset(), email);
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize/publish verification code. email={}, {}", email, e.getMessage());
        }
    }
}