package ru.practicum.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.grpc.stats.collector.UserActionControllerGrpc;

@Component
public class CollectorClient {
    @GrpcClient("collector")
    UserActionControllerGrpc.UserActionControllerBlockingStub collector;

    public void newUserAction(UserActionProto actionProto) {
        collector.collectUserAction(actionProto);
    }
}
