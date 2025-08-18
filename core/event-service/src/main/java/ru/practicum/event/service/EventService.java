package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.*;

import java.util.List;

public interface EventService {

    List<EventShortDto> getUserEvents(Long userId,
                                      Pageable pageable);

    EventFullDto createEvent(Long userId,
                             NewEventDto newEventDto);

    EventFullDto getEventByIdAndInitiator(Long userId,
                                          Long eventId);

    EventFullDto getEventFullDtoById(Long eventId);


    EventFullDto updateUserEvent(Long userId,
                                 Long eventId,
                                 UpdateEventUserRequest updateEventUserRequest);

    List<EventFullDto> searchEventsByAdmin(SearchAdminEventsParamDto searchAdminEventsParamDto);

    EventFullDto updateEventByAdmin(Long eventId,
                                    UpdateEventAdminRequest updateEventAdminRequest);

    List<EventShortDto> searchPublicEvents(SearchPublicEventsParamDto searchPublicEventsParamDto);

    EventFullDto getPublicEvent(Long eventId,
                                HttpServletRequest request);

    EventFullDto increaseConfirmed(Long eventId, Integer quantity);

}
