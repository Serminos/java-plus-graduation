package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AggregatorService;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaClient {
    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "${kafka.topic.stats.v1}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenActions(UserActionAvro actionAvro) {
        log.info("Получили действие из кафки: {}", actionAvro);
        aggregatorService.processAction(actionAvro);
    }
}
