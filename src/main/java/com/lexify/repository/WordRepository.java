package com.lexify.repository;

import com.lexify.model.Word;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface WordRepository extends JpaRepository<Word, Long> {

    @EntityGraph(attributePaths = "dictionary")
    List<Word> findByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = "dictionary")
    Optional<Word> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndRussianTextIgnoreCase(Long userId, String russianText);

    boolean existsByUserIdAndEnglishTextIgnoreCase(Long userId, String englishText);
}
