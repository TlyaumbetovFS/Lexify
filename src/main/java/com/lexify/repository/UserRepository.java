package com.lexify.repository;

import com.lexify.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.stream.Stream;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select item.id from User item")
    Stream<Long> streamIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(Long id);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
