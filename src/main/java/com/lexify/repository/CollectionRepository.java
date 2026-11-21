package com.lexify.repository;

import com.lexify.model.WordCollection;
import com.lexify.dto.CollectionSummary;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<WordCollection, Long> {

    @Query("""
            select new com.lexify.dto.CollectionSummary(c.id, c.name, c.description, cast(size(c.words) as long))
            from WordCollection c where c.user.id = :userId order by c.id desc
            """)
    List<CollectionSummary> summaries(Long userId);

    @EntityGraph(attributePaths = {"words", "words.dictionary"})
    List<WordCollection> findByUserIdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = {"words", "words.dictionary"})
    Optional<WordCollection> findByIdAndUserId(Long id, Long userId);
}
