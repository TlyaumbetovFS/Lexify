package com.lexify.repository;

import com.lexify.model.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.stream.Stream;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("select item.id from ChatMessage item")
    Stream<Long> streamIds();

    List<ChatMessage> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);

    Optional<ChatMessage> findByIdAndRoomId(Long id, Long roomId);
}
