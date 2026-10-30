package com.lexify.repository;

import com.lexify.model.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<ChatRoom, Long> {

    @EntityGraph(attributePaths = "moderators")
    List<ChatRoom> findAllByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = "moderators")
    Optional<ChatRoom> findById(Long id);

    boolean existsByIdAndModeratorsId(Long id, Long userId);
}
