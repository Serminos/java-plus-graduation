package ru.practicum.comment.service;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.api.EventApi;
import ru.practicum.api.UserApi;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.dto.comment.CommentRequestDto;
import ru.practicum.dto.comment.CommentResponseDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImpl implements CommentService {

    CommentRepository commentRepository;

    UserApi userApi;

    EventApi eventApi;

    @Override
    public List<CommentResponseDto> findAll(Long userId,
                                            Long eventId,
                                            PageRequest pageRequest) {
        Long initiator = getUserById(userId);
        EventFullDto eventFullDto = getEventById(eventId);
        List<Comment> comments = commentRepository.findByAuthorAndEventId(initiator, eventFullDto.getId(), pageRequest);
        return comments.stream().map(CommentMapper::toCommentResponseDto).toList();
    }

    @Override
    public CommentResponseDto save(CommentRequestDto commentRequestDto,
                                   Long userId,
                                   Long eventId) {
        Long initiator = getUserById(userId);
        EventFullDto event = getEventById(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя написать комментарий к событию которое еще не было опубликованно");
        }
        Comment comment = commentRepository.save(CommentMapper.toComment(commentRequestDto, initiator, event.getId()));
        return CommentMapper.toCommentResponseDto(comment);
    }

    @Override
    public CommentResponseDto update(CommentRequestDto commentRequestDto,
                                     Long userId,
                                     Long commentId) {
        Comment oldComment = getCommentById(commentId);
        getUserById(userId);

        if (!oldComment.getAuthor().equals(userId)) {
            throw new ConflictException("Редактировать комментарии разрешено только его автору");
        }
        oldComment.setText(commentRequestDto.getText());
        Comment comment = commentRepository.save(oldComment);
        return CommentMapper.toCommentResponseDto(comment);
    }

    @Override
    public void delete(Long userId,
                       Long commentId) {
        Comment comment = getCommentById(commentId);
        getUserById(userId);
        EventFullDto eventFullDto = getEventById(comment.getEventId());

        if (!comment.getAuthor().equals(userId) &&
                !comment.getAuthor().equals(eventFullDto.getInitiator())) {
            throw new ConflictException("Удалять комментарии разрешено только его автору или инициатору мероприятия");
        }
        commentRepository.deleteById(commentId);
    }


    @Override
    public void deleteByIds(final List<Long> ids) {
        List<Comment> events = commentRepository.findAllById(ids);
        if (ids.size() != events.size()) {
            throw new ValidationException("Были переданы несуществующие id событий");
        }
        commentRepository.deleteAllById(ids);
        log.info("Комментарии успешно удалены");
    }

    @Override
    public void deleteByEventId(Long eventId) {
        EventFullDto eventFullDto = getEventById(eventId);
        commentRepository.deleteByEventId(eventFullDto.getId());
        log.info("Все комментарии у события с id = {} успешно удалены", eventId);
    }

    @Override
    public List<CommentResponseDto> findByEvent(Long eventId,
                                                PageRequest pageRequest) {
        EventFullDto eventFullDto = getEventById(eventId);
        List<Comment> comments = commentRepository.findByEventId(eventId, pageRequest);
        log.info("Получены все комментарии события с id = {}", eventId);
        return comments.stream().map(CommentMapper::toCommentResponseDto).toList();
    }

    @Override
    public CommentResponseDto findById(final Long commentId) {
        Comment comment = getCommentById(commentId);
        return CommentMapper.toCommentResponseDto(comment);
    }

    private Long getUserById(Long userId) {
        try {
            return userApi.getUserById(userId).getId();
        } catch (FeignException e) {
            new NotFoundException("Не найден пользователя с ID: " + userId);
            return null;
        }
    }


    private EventFullDto getEventById(Long eventId) {
        try {
            return eventApi.getEventFullDtoById(eventId);
        } catch (FeignException e) {
            new NotFoundException("Не найдено событие с ID: " + eventId);
            return null;
        }
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментария с id = {} нет." + commentId));
    }
}
