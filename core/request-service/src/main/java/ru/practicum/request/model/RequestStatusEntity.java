package ru.practicum.request.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.practicum.dto.request.RequestStatus;

@Entity
@Table(name = "request_statuses")
@Getter
@NoArgsConstructor
public class RequestStatusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RequestStatus name;
}
