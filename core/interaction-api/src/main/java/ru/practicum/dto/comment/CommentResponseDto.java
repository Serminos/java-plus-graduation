package ru.practicum.dto.comment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponseDto {
    private Long id;
    private String text;
    private Long authorId;
    private Long eventId;
    private LocalDateTime created;
}
