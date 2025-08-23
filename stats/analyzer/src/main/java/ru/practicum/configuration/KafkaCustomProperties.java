package ru.practicum.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaCustomProperties {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    @Value(value = "${spring.kafka.consumer.user-actions-consumer.group-id}")
    private String userActionGroupId;

    @Value(value = "${spring.kafka.consumer.events-similarity-consumer.group-id}")
    private String similarityGroupId;

    @Value(value = "${spring.kafka.consumer.user-actions-consumer.value-deserializer}")
    private String userActionDeserializer;

    @Value(value = "${spring.kafka.consumer.events-similarity-consumer.value-deserializer}")
    private String similarityDeserializer;

    public ConsumerFactory<String, UserActionAvro> userActionAvroConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, userActionGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, userActionDeserializer);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> userActionListener() {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory
                = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionAvroConsumerFactory());
        return factory;
    }

    public ConsumerFactory<String, EventSimilarityAvro> similarityConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, similarityGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, similarityDeserializer);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> similarityListener() {
        ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> factory
                = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(similarityConsumerFactory());
        return factory;
    }
}
