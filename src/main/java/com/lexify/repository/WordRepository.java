package com.lexify.repository;

import com.lexify.model.Word;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface WordRepository extends JpaRepository<Word, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Word w where w.id = :id and w.user.id = :userId")
    Optional<Word> lockOwned(Long userId, Long id);

    @Query("""
            select w from Word w left join StudyCard s on s.word = w and s.direction = :direction
            where w.user.id = :userId
              and (:collectionId is null or exists (select c.id from WordCollection c join c.words cw
                   where c.id = :collectionId and cw = w))
              and (s.id is null or s.due <= :now)
            order by case when s.id is null then 1 else 0 end, s.due, w.id
            """)
    List<Word> studyQueue(Long userId, String direction, Long collectionId, Instant now, Pageable pageable);

    @Query("""
            select count(w), sum(case when s.id is null then 1 else 0 end),
                   sum(case when s.due <= :now then 1 else 0 end), min(s.due)
            from Word w left join StudyCard s on s.word = w and s.direction = :direction
            where w.user.id = :userId
              and (:collectionId is null or exists (select c.id from WordCollection c join c.words cw
                   where c.id = :collectionId and cw = w))
            """)
    List<Object[]> studyCounts(Long userId, String direction, Long collectionId, Instant now);

    long countByUserId(Long userId);

    @Query(value = "select coalesce(sum(octet_length(english_text) + octet_length(russian_text) + "
            + "coalesce(octet_length(example), 0)), 0) from words where user_id = :userId", nativeQuery = true)
    long textBytes(Long userId);

    @Query("select word.englishText from Word word")
    List<String> findEnglishTexts();

    @EntityGraph(attributePaths = "dictionary")
    List<Word> findByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = "dictionary")
    Optional<Word> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndRussianTextIgnoreCase(Long userId, String russianText);

    boolean existsByUserIdAndEnglishTextIgnoreCase(Long userId, String englishText);
}
