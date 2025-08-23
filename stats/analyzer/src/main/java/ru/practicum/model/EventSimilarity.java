package ru.practicum.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Builder
@Table(name = "event_similarity")
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    Long id;

    @Column(name = "event_A_id")
    Long eventAId;

    @Column(name = "event_B_id")
    Long eventBId;

    @Column(name = "score")
    Double score;

    @Column(name = "calculated_at")
    LocalDateTime calculatedAt;
}
