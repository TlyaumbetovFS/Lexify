package com.lexify.repository;

import com.lexify.model.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.stream.Stream;

public interface RoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("select item.id from ChatRoom item")
    Stream<Long> streamIds();

    @EntityGraph(attributePaths = "moderators")
    List<ChatRoom> findAllByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = "moderators")
    Optional<ChatRoom> findById(Long id);

    boolean existsByIdAndModeratorsId(Long id, Long userId);
}
