package com.lexify.repository;

import com.lexify.model.Word;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
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

    @EntityGraph(attributePaths = "dictionary")
    @Query("""
            select w from Word w where w.user.id = :userId
              and (:query = '' or locate(lower(:query),
                   lower(concat(w.russianText, ' ', w.englishText, ' ', coalesce(w.example, '')))) > 0)
              and (:collectionId is null or
                   case when exists (select c.id from WordCollection c join c.words cw
                            where c.id = :collectionId and c.user.id = :userId and cw = w) then false else true end = :exclude)
            order by w.id desc
            """)
    Page<Word> searchPage(Long userId, String query, Long collectionId, boolean exclude, Pageable pageable);

    long countByUserId(Long userId);

    @Query(value = "select coalesce(sum(octet_length(english_text) + octet_length(russian_text) + "
            + "coalesce(octet_length(example), 0) + coalesce(octet_length(russian_hint), 0)), 0) from words where user_id = :userId", nativeQuery = true)
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
