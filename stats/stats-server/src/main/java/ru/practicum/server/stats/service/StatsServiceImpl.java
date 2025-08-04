package ru.practicum.server.stats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.server.stats.exceptions.ValidationException;
import ru.practicum.server.stats.mapper.StatsMapper;
import ru.practicum.server.stats.model.App;
import ru.practicum.server.stats.model.Uri;
import ru.practicum.server.stats.repository.AppRepository;
import ru.practicum.server.stats.repository.StatsRepository;
import ru.practicum.server.stats.repository.UriRepository;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;
    private final AppRepository appRepository;
    private final UriRepository uriRepository;

    @Transactional
    @Override
    public EndpointHitDto saveHit(EndpointHitDto endpointHitDto) {
        // Получаем приложение или создаем новое
        App app = getOrCreateApp(endpointHitDto);
        // Получаем URI или создаем новый
        Uri uri = getOrCreateUri(endpointHitDto);
        // Преобразуем DTO в Entity и сохраняем
        return StatsMapper.toDto(statsRepository.save(StatsMapper.toEntity(endpointHitDto, app, uri)));
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris) {
        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }
        return statsRepository.getStats(start, end, uris);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }
        if (unique) {
            return statsRepository.getUniqueStats(start, end, uris);
        } else {
            return statsRepository.getStats(start, end, uris);
        }
    }

    private App getOrCreateApp(EndpointHitDto endpointHitDto) {
        return appRepository.findByName(endpointHitDto.getApp())
                .orElseGet(() -> appRepository.save(new App(null, endpointHitDto.getApp())));
    }

    private Uri getOrCreateUri(EndpointHitDto endpointHitDto) {
        return uriRepository.findByUri(endpointHitDto.getUri())
                .orElseGet(() -> uriRepository.save(new Uri(null, endpointHitDto.getUri())));
    }
}