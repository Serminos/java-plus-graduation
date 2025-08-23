package ru.practicum.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "user_actions")
public class UserAction {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    Long id;

    @Column(name = "user_id")
    Long userId;

    @Column(name = "event_id")
    Long eventId;

    @Column(name = "score")
    Double score;

    @Column(name = "interact_at")
    LocalDateTime interactAt;
}
