package ru.practicum.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dal.RecommendedEventI;
import ru.practicum.dal.SimilarityRepository;
import ru.practicum.dal.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.event.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.grpc.stats.event.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.event.UserPredictionsRequestProto;
import ru.practicum.mappers.EventSimilarityMapper;
import ru.practicum.mappers.UserActionMapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzerServiceImpl implements AnalyzerService {
    private final UserActionRepository userActionRepository;
    private final SimilarityRepository similarityRepository;

    @Override
    public void saveAction(UserActionAvro actionAvro) {
        if (userActionRepository.existsUserActionByEventIdAndUserId(actionAvro.getEventId(), actionAvro.getUserId())) {
            UserAction action = userActionRepository.findByEventIdAndUserId(actionAvro.getEventId(), actionAvro.getUserId());
            if (action.getScore() < getActionRating(actionAvro.getActionType())) {
                userActionRepository.delete(action);
                userActionRepository.save(UserActionMapper.INSTANCE.toAction(actionAvro));
            }
        } else {
            userActionRepository.save(UserActionMapper.INSTANCE.toAction(actionAvro));
        }
        log.info("Сохранили действие {}", actionAvro);
    }


    @Override
    public void saveSimilarity(EventSimilarityAvro similarityAvro) {
        if (similarityRepository.existsEventSimilaritiesByEventAIdAndEventBId(similarityAvro.getEventAId(), similarityAvro.getEventBId())) {
            EventSimilarity eventSimilarity = similarityRepository.findByEventAIdAndEventBId(similarityAvro.getEventAId(), similarityAvro.getEventBId());
            if (eventSimilarity.getScore() < similarityAvro.getScore()) {
                similarityRepository.delete(eventSimilarity);
                similarityRepository.save(EventSimilarityMapper.INSTANCE.toEventSimilarity(similarityAvro));
            }
        } else {
            similarityRepository.save(EventSimilarityMapper.INSTANCE.toEventSimilarity(similarityAvro));
        }
        log.info("Сохранили похожесть {}", similarityAvro);
    }

    @Override
    public void getRecommendations(UserPredictionsRequestProto request,StreamObserver<RecommendedEventProto> responseObserver) {
        List<RecommendedEventI> events = similarityRepository.getRecommended(request.getUserId(), request.getMaxResults());
        sendResponse(events.stream()
                .map(UserActionMapper.INSTANCE::toProto)
                .toList(), responseObserver);
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        for (Long event : request.getEventIdList()) {
            List<RecommendedEventI> events = userActionRepository.getInteractions(event);
            sendResponse(events.stream()
                    .map(UserActionMapper.INSTANCE::toProto)
                    .toList(), responseObserver);

        }
    }

    @Override
    public void getSimilarities(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        List<RecommendedEventI> events = similarityRepository.getSimilarities(request.getEventId(), request.getUserId(), request.getMaxResults());
        sendResponse(events.stream()
                .map(UserActionMapper.INSTANCE::toProto)
                .toList(), responseObserver);
    }

    private void sendResponse(List<RecommendedEventProto> response, StreamObserver<RecommendedEventProto> responseObserver) {
        response.forEach(responseObserver::onNext);
    }

    private double getActionRating(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
