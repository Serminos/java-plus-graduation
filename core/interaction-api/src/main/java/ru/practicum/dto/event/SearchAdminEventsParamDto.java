package ru.practicum.dto.event;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SearchAdminEventsParamDto {
    private List<Long> users;
    private List<EventState> eventStates;
    private List<Long> categoriesIds;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private PageRequest pageRequest;
}
