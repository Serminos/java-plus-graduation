package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {
    @Value("${collector.topic.stats.v1}")
    private String topic;

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Override
    public void newUserAction(UserActionProto actionProto) {
        UserActionAvro actionAvro = new UserActionAvro();
        actionAvro.setEventId(actionProto.getEventId());
        actionAvro.setUserId(actionProto.getUserId());
        actionAvro.setActionType(getAvroType(actionProto.getActionType()));
        actionAvro.setTimestamp(Instant.ofEpochSecond(actionProto.getTimestamp().getSeconds(), actionProto.getTimestamp().getNanos()));
        kafkaTemplate.send(topic, actionAvro);
    }

    private ActionTypeAvro getAvroType(ActionTypeProto proto) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            default -> throw new IllegalArgumentException("Неизвестное действие: " + proto);
        };
    }
}
