package ru.practicum.dto.comment;

public record CommentFilter(
        Long authorId,
        Long eventId
) {}
