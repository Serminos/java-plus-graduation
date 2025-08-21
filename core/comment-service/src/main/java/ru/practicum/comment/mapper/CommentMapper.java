package ru.practicum.comment.mapper;

import ru.practicum.comment.model.Comment;
import ru.practicum.dto.comment.CommentRequestDto;
import ru.practicum.dto.comment.CommentResponseDto;

import java.time.LocalDateTime;

public class CommentMapper {

    public static Comment toComment(CommentRequestDto commentRequestDto,
                                    Long userId,
                                    Long evenId) {
        return Comment.builder()
                .text(commentRequestDto.getText())
                .created(LocalDateTime.now())
                .author(userId)
                .eventId(evenId).build();
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment) {

        return CommentResponseDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthor())//.getName()
                .eventId(comment.getEventId())
                .created(comment.getCreated())
                .build();
    }
}
