package ru.practicum.service;

import io.grpc.stub.StreamObserver;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.event.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.grpc.stats.event.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.event.UserPredictionsRequestProto;

public interface AnalyzerService {

    void saveAction(UserActionAvro actionAvro);

    void saveSimilarity(EventSimilarityAvro similarityAvro);

    void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver);

    public void getSimilarities(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver);

    public void getRecommendations(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver);
}
