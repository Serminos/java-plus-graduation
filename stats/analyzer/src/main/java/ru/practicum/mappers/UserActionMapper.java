package ru.practicum.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.practicum.dal.RecommendedEventI;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.event.RecommendedEventProto;
import ru.practicum.model.UserAction;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper
public interface UserActionMapper {

    UserActionMapper INSTANCE = Mappers.getMapper(UserActionMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "interactAt", qualifiedByName = "timestampConvert", source = "timestamp")
    @Mapping(target = "score", qualifiedByName = "getActionRating", source = "actionType")
    UserAction toAction(UserActionAvro actionAvro);

    @Named("timestampConvert")
    default LocalDateTime timestampConvert(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    @Named("getActionRating")
    default double getActionRating(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "score", source = "score")
    RecommendedEventProto toProto(RecommendedEventI ri);
}
