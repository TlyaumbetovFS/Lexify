package com.lexify.repository;

import com.lexify.model.DictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface DictionaryRepository extends JpaRepository<DictionaryEntry, String> {

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    <S extends DictionaryEntry> S saveAndFlush(S entry);
}
