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
import ru.practicum.dto.comment.CommentFilter;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImpl implements CommentService {

    CommentRepository commentRepository;

    UserApi userApi;

    EventApi eventApi;

    @Override
    public List<CommentResponseDto> findAllByAuthorAndEvent(CommentFilter filter,
                                            PageRequest pageRequest) {
        Long initiator = getUserById(filter.authorId());
        EventFullDto eventFullDto = getEventById(filter.eventId());
        return findCommentsByAuthorAndEvent(initiator, eventFullDto.getId(), pageRequest);
    }

    @Transactional(readOnly = true)
    private List<CommentResponseDto> findCommentsByAuthorAndEvent(Long authorId, Long eventId, PageRequest pageRequest) {
        List<Comment> comments = commentRepository.findByAuthorAndEventId(authorId, eventId, pageRequest);
        return comments.stream().map(CommentMapper::toCommentResponseDto).toList();
    }

    @Override
    public CommentResponseDto save(CommentRequestDto commentRequestDto, Long userId, Long eventId) {
        Long initiator = getUserById(userId);
        EventFullDto event = getEventById(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя написать комментарий к неопубликованному событию");
        }

        return saveComment(commentRequestDto, initiator, event.getId());
    }

    @Transactional
    private CommentResponseDto saveComment(CommentRequestDto dto, Long author, Long eventId) {
        Comment comment = commentRepository.save(CommentMapper.toComment(dto, author, eventId));
        return CommentMapper.toCommentResponseDto(comment);
    }

    @Override
    public CommentResponseDto update(CommentRequestDto commentRequestDto, Long userId, Long commentId) {
        getUserById(userId);
        return updateComment(commentRequestDto, userId, commentId);
    }

    @Transactional
    private CommentResponseDto updateComment(CommentRequestDto dto, Long userId, Long commentId) {
        Comment comment = getCommentById(commentId);

        if (!comment.getAuthor().equals(userId)) {
            throw new ConflictException("Редактирование разрешено только автору");
        }

        comment.setText(dto.getText());
        return CommentMapper.toCommentResponseDto(comment);
    }

    @Override
    public void delete(Long userId,
                       Long commentId) {
        getUserById(userId);
        Comment comment = getCommentById(commentId);
        EventFullDto eventFullDto = getEventById(comment.getEventId());

        if (!userId.equals(comment.getAuthor()) &&
                !userId.equals(eventFullDto.getInitiator())) {
            throw new ConflictException("Удалять комментарии разрешено только его автору или инициатору мероприятия");
        }
        deleteInTransaction(commentId);
    }

    @Transactional
    void deleteInTransaction(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
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
        deleteCommentsByEvent(eventFullDto.getId());
        log.info("Все комментарии у события с id = {} успешно удалены", eventId);
    }

    @Transactional
    void deleteCommentsByEvent(Long eventId) {
        commentRepository.deleteByEventId(eventId);
        log.info("Все комментарии у события с id = {} успешно удалены", eventId);
    }

    @Override
    public List<CommentResponseDto> findByEvent(Long eventId,
                                                PageRequest pageRequest) {
        EventFullDto eventFullDto = getEventById(eventId);
        return findCommentsByEvent(eventFullDto.getId(), pageRequest);
    }

    @Transactional(readOnly = true)
    List<CommentResponseDto> findCommentsByEvent(Long eventId, PageRequest pageRequest) {
        List<Comment> comments = commentRepository.findByEventId(eventId, pageRequest);
        log.info("Получены все комментарии события с id = {}", eventId);
        return comments.stream().map(CommentMapper::toCommentResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponseDto findById(final Long commentId) {
        return CommentMapper.toCommentResponseDto(getCommentById(commentId));
    }

    private Long getUserById(Long userId) {
        try {
            return userApi.getUserById(userId).getId();
        } catch (FeignException e) {
            throw new NotFoundException("Не найден пользователя с ID: " + userId);
        }
    }


    private EventFullDto getEventById(Long eventId) {
        try {
            return eventApi.getEventFullDtoById(eventId);
        } catch (FeignException e) {
            throw new NotFoundException("Не найдено событие с ID: " + eventId);
        }
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментария с id = {} нет." + commentId));
    }
}
