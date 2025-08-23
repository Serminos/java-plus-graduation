package ru.practicum.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.model.UserAction;

import java.util.List;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    UserAction findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsUserActionByEventIdAndUserId(Long eventId, Long userId);

    @Query(value = """
            select event_id as event_id, sum(score) as score
            from user_actions
            where event_id = :eventId
            group by event_id""", nativeQuery = true)
    List<RecommendedEventI> getInteractions(Long eventId);
}
