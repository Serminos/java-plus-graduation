package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.Event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AggregatorServiceImpl implements AggregatorService {
    private final Map<Long, Map<Long, Double>> weightMap;
    private final Map<Long, Double> weightSumMap;
    private final Map<Long, Map<Long, Double>> minWeightsSums;
    private final Map<Long, List<Long>> userEvents;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topic.similarity.v1}")
    private String similarityTopic;

    public AggregatorServiceImpl(KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate) {
        this.weightMap = new HashMap<>();
        this.weightSumMap = new HashMap<>();
        this.minWeightsSums = new HashMap<>();
        this.userEvents = new HashMap<>();
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void processAction(UserActionAvro actionAvro) {
        long userId = actionAvro.getUserId();
        long eventId = actionAvro.getEventId();
        double newRating = getActionRating(actionAvro.getActionType());
        if (!userEvents.containsKey(userId)) {
            log.info("Пришел новый пользователь {}", userId);
            if (!weightMap.containsKey(eventId)) {
                log.info("Пришло новое мероприятие {}, вес {}", eventId, newRating);
                weightSumMap.put(eventId, newRating);
                weightMap.put(eventId, Map.of(userId, newRating));
                userEvents.put(userId, List.of(eventId));
                log.info("Пишем новый суммарный вес {}", weightSumMap.get(eventId));
                log.info("Пишем новый вес в таблицу взаимодействий {}", weightMap.get(eventId).get(userId));
            } else {
                log.info("Мероприятие {} уже есть, суммарный вес {}, вес нового действия {}", eventId, weightSumMap.get(eventId), newRating);
                weightSumMap.replace(eventId, weightSumMap.get(eventId) + newRating);
                log.info("Пишем новый суммарный вес {}", weightSumMap.get(eventId));
                Map<Long, Double> users = new HashMap<>(weightMap.get(eventId));
                users.put(userId, newRating);
                weightMap.replace(eventId, users);
                log.info("Пишем новый вес в таблицу взаимодействий {}", weightMap.get(eventId).get(userId));
                userEvents.put(userId, List.of(eventId));
            }
            log.info("Сохранили, ничего обновлять не надо");
        } else {
            if (!userEvents.get(userId).contains(eventId)) {
                if (!weightMap.containsKey(eventId)) {
                    log.info("Пользователь {} есть, а мероприятия {} нет, создаем", userId, eventId);
                    log.info("Пришло новое мероприятие {}, вес {}", eventId, newRating);
                    weightSumMap.put(eventId, newRating);
                    log.info("Пишем новый суммарный вес {}", weightSumMap.get(eventId));
                    weightMap.put(eventId, Map.of(userId, newRating));
                    log.info("Пишем новый вес в таблицу взаимодействий {}", weightMap.get(eventId).get(userId));
                    List<Long> events = new ArrayList<>(userEvents.get(userId));
                    events.add(eventId);
                    userEvents.replace(userId, events);
                    calcMin(eventId, userId, newRating);
                    sendToKafka(calcSim(eventId, userId), actionAvro.getTimestamp());
                } else {
                    log.info("Пользователь {} еще не взаимодействовал с мероприятием {}", userId, eventId);
                    log.info("Мероприятие {} уже есть, суммарный вес {}, вес нового действия {}", eventId, weightSumMap.get(eventId), newRating);
                    weightSumMap.replace(eventId, weightSumMap.get(eventId) + newRating);
                    log.info("Пишем новый суммарный вес {}", weightSumMap.get(eventId));
                    List<Long> events = new ArrayList<>(userEvents.get(userId));
                    events.add(eventId);
                    userEvents.replace(userId, events);
                    calcMin(eventId, userId, newRating);
                    log.info("Старый вес действия пользователя {}", weightMap.get(eventId).get(userId));
                    Map<Long, Double> users = new HashMap<>(weightMap.get(eventId));
                    users.put(userId, newRating);
                    weightMap.replace(eventId, users);
                    log.info("Пишем новый вес в таблицу взаимодействий {}", weightMap.get(eventId).get(userId));
                    sendToKafka(calcSim(eventId, userId), actionAvro.getTimestamp());
                }

            } else {
                log.info("Пользователь {} уже взаимодействовал с мероприятием {}", userId, eventId);
                if (weightMap.get(eventId).get(userId) < newRating) {
                    log.info("Мероприятие {} уже есть, суммарный вес {}, вес нового действия {}", eventId, weightSumMap.get(eventId), newRating);
                    weightSumMap.replace(eventId, weightSumMap.get(eventId) + (newRating - weightMap.get(eventId).get(userId)));
                    log.info("Пишем новый суммарный вес {}", weightSumMap.get(eventId));
                    calcMin(eventId, userId, newRating);
                    log.info("Старый вес действия пользователя {}", weightMap.get(eventId).get(userId));
                    Map<Long, Double> users = new HashMap<>(weightMap.get(eventId));
                    users.replace(userId, newRating);
                    weightMap.replace(eventId, users);
                    log.info("Пишем новый вес в таблицу взаимодействий {}", weightMap.get(eventId).get(userId));
                    sendToKafka(calcSim(eventId, userId), actionAvro.getTimestamp());
                }
            }
        }
    }

    private void calcMin(long eventId, long userId, double newRating) {
        List<Long> events = new ArrayList<>(userEvents.get(userId));
        events.remove(eventId);
        for (Long event : events) {
            if (get(eventId, event) == 0) {
                log.info("Создаем новую минимальную сумму {} и {}", eventId, event);
                log.info("Выбираем меньшее из {} и {}", weightMap.get(event).get(userId), newRating);
                double sum = Math.min(weightMap.get(event).get(userId), newRating);
                put(eventId, event, sum);
                log.info("Создали минимальную сумму для {} и {} равную {}", eventId, event, sum);
            } else {
                if (!weightMap.get(eventId).containsKey(userId)) {
                    log.info("Добавляем в минимальную сумму {} и {} новый вес {}", eventId, event, newRating);
                    log.info("Текущая минимальная сумма для {} и {} равна {}", eventId, event, get(eventId, event));
                    log.info("Выбираем меньшее из {} и {}", weightMap.get(event).get(userId), newRating);
                    double min = Math.min(weightMap.get(event).get(userId), newRating);
                    log.info("Добавляем к {} выбраный минимум {}", get(eventId, event), min);
                    put(eventId, event, get(eventId, event) + min);
                    log.info("Новая минимальная сумма для {} и {} равна {}", eventId, event, get(eventId, event));
                } else {
                    log.info("Меняем минимальную сумму {} и {}, новый вес {}", eventId, event, newRating);
                    log.info("Старая минимальная сумма {}", get(eventId, event));
                    double oldMin = Math.min(weightMap.get(eventId).get(userId), weightMap.get(event).get(userId));
                    log.info("Старый минимум {}", oldMin);
                    double newMin = Math.min(newRating, weightMap.get(event).get(userId));
                    log.info("Новый минимум {}", newMin);
                    double delta = newMin - oldMin;
                    log.info("Дельта {}", delta);
                    if (delta > 0)
                        put(eventId, event, get(eventId, event) + delta);
                    log.info("Новая минимальная сумма для {} и {} равна {}", eventId, event, get(eventId, event));
                }
            }
        }

    }

    private List<Event> calcSim(long eventA, long userId) {
        List<Event> events = new ArrayList<>();
        List<Long> userEvent = new ArrayList<>(userEvents.get(userId));
        userEvent.remove(eventA);
        for (Long event : userEvent) {
            double sum = get(eventA, event) /
                    (Math.sqrt(weightSumMap.get(eventA)) * Math.sqrt(weightSumMap.get(event)));
            Event ev = new Event();
            if (eventA < event) {
                ev.setEventA(eventA);
                ev.setEventB(event);
                log.info("New similarity between {} and {} is {}", eventA, event, sum);
            } else {
                ev.setEventA(event);
                ev.setEventB(eventA);
                log.info("New similarity between {} and {} is {}", event, eventA, sum);
            }
            ev.setSum(sum);
            events.add(ev);
        }
        return events;
    }


    private Double getActionRating(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    private double get(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private void put(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    private void sendToKafka(List<Event> events, Instant ts) {
        for (Event event : events) {
            EventSimilarityAvro kafkaEvent = new EventSimilarityAvro();
            kafkaEvent.setEventAId(event.getEventA());
            kafkaEvent.setEventBId(event.getEventB());
            kafkaEvent.setScore(event.getSum());
            kafkaEvent.setTimestamp(ts);
            log.info("Отправляем событие в кафку {}", kafkaEvent);
            kafkaTemplate.send(similarityTopic, kafkaEvent);
        }
    }
}
