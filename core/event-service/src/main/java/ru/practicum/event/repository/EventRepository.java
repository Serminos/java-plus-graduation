package ru.practicum.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.event.model.Event;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findByInitiatorId(Long initiatorId,
                                  Pageable pageable);

    @Modifying
    @Query("UPDATE Event e SET e.confirmedRequests = e.confirmedRequests + :delta WHERE e.id = :eventId")
    void incrementConfirmedRequests(
            @Param("eventId") Long eventId,
            @Param("delta") Integer delta
    );
}
