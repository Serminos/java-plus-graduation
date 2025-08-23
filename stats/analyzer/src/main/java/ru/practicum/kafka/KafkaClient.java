package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AnalyzerService;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaClient {
    private final AnalyzerService analyzerService;

    @KafkaListener(topics = "${kafka.topic.stats.v1}",
            groupId = "${spring.kafka.consumer.user-actions-consumer.group-id}",
            containerFactory = "userActionListener")
    public void listenActions(UserActionAvro actionAvro) {
        log.info("Получили действие {}", actionAvro);
        analyzerService.saveAction(actionAvro);
    }

    @KafkaListener(topics = "${kafka.topic.similarity.v1}",
            groupId = "${spring.kafka.consumer.events-similarity-consumer.group-id}",
            containerFactory = "similarityListener")
    public void listenSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        log.info("Получили новую похожесть: {}", eventSimilarityAvro);
        analyzerService.saveSimilarity(eventSimilarityAvro);
    }
}
