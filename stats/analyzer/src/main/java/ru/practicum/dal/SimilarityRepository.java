package ru.practicum.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.model.EventSimilarity;

import java.util.List;

@Repository
public interface SimilarityRepository extends JpaRepository<EventSimilarity, Long> {
    boolean existsEventSimilaritiesByEventAIdAndEventBId(Long eventA, Long eventB);

    EventSimilarity findByEventAIdAndEventBId(Long eventA, Long eventB);

    @Query(value = """
            select event_b_id, score
            from event_similarity
            where event_a_id = :event and event_b_id in
                (select event_id from user_actions
                where user_id <> :userId and event_id <> :event)
            order by score desc
            limit :limit""", nativeQuery = true)
    List<RecommendedEventI> getSimilarities(Long event, Long userId, Integer limit);

    @Query(value = """
            with user_events as (select event_id from user_actions where user_id = :userId)
            select event_b_id as eventId, max(score) as score
            from event_similarity
            where event_a_id in (select event_id from user_events)
            and event_b_id not in (select event_id from user_events)
            group by event_b_id
            order by max(score) desc
            limit :limit""", nativeQuery = true)
    List<RecommendedEventI> getRecommended(Long userId, Integer limit);
}
