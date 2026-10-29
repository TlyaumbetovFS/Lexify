package com.lexify.repository;

import com.lexify.model.Word;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByUserIdOrderByIdDesc(Long userId);

    Optional<Word> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndRussianText(Long userId, String russianText);
}
