package com.lexify.repository;

import com.lexify.model.WordCollection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<WordCollection, Long> {

    @EntityGraph(attributePaths = "words")
    List<WordCollection> findByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = "words")
    Optional<WordCollection> findByIdAndUserId(Long id, Long userId);
}
