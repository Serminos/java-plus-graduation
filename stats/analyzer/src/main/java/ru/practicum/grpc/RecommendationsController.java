package ru.practicum.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.grpc.stats.event.*;
import ru.practicum.service.AnalyzerService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    public final AnalyzerService analyzerService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto userPredictionsRequestProto,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Пришел запрос на рекомендации для пользователя {}", userPredictionsRequestProto.getUserId());
        try {
            analyzerService.getRecommendations(userPredictionsRequestProto, responseObserver);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto similarEventsRequestProto,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Пришел запрос на похожесть на событие {} для пользователя {}",
                similarEventsRequestProto.getEventId(), similarEventsRequestProto.getUserId());
        try {
            analyzerService.getSimilarities(similarEventsRequestProto, responseObserver);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto interactionsCountRequestProto,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Пришел запрос на взаимодействие на события");
        try {
            analyzerService.getInteractionsCount(interactionsCountRequestProto, responseObserver);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
