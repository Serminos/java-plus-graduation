package ru.practicum.service;


import ru.practicum.grpc.stats.action.UserActionProto;

public interface CollectorService {

    void newUserAction(UserActionProto actionProto);
}
