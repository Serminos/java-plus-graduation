package ru.practicum.user.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.user.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    @Query("""
            SELECT u
            FROM User as u
            WHERE (:ids IS NULL OR u.id in :ids)
            """)
    List<User> findAllByFilter(List<Long> ids, Pageable pageable);
}
