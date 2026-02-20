package org.example.authservice.config;

import lombok.RequiredArgsConstructor;
import org.example.authservice.dto.event.SendCodeEvent;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, SendCodeEvent> producerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties();

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, SendCodeEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}