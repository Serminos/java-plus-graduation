package ru.practicum.request.service;

import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;

import java.util.List;
import java.util.Map;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createParticipationRequest(Long userId,
                                                       Long eventId);

    ParticipationRequestDto cancelParticipationRequest(Long userId,
                                                       Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId,
                                                   Long eventId);

    Map<String, List<ParticipationRequestDto>> approveRequests(Long userId,
                                                               Long eventId,
                                                               EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest);

}
