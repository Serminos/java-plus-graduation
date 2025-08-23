package ru.practicum.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.EventSimilarity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper
public interface EventSimilarityMapper {
    EventSimilarityMapper INSTANCE = Mappers.getMapper(EventSimilarityMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "calculatedAt", qualifiedByName = "timestampConvert", source = "timestamp")
    EventSimilarity toEventSimilarity(EventSimilarityAvro eventSimilarityAvro);

    @Named("timestampConvert")
    default LocalDateTime timestampConvert(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
