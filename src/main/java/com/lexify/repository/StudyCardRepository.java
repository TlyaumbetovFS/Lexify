package com.lexify.repository;

import com.lexify.model.StudyCard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyCardRepository extends JpaRepository<StudyCard, Long> {
    Optional<StudyCard> findByWordIdAndDirection(Long wordId, String direction);
}
