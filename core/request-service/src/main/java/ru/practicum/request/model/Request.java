package ru.practicum.request.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "participation_requests")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode()
@Builder
public class Request {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id")
    private Long requesterId;

    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private RequestStatusEntity status;

    @Column(name = "created")
    private LocalDateTime created;
}

