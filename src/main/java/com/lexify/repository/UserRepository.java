package com.lexify.repository;

import com.lexify.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.stream.Stream;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select item.id from User item")
    Stream<Long> streamIds();

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
