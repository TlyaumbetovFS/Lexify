package com.lexify.service;

import com.lexify.dto.CollectionRequest;
import com.lexify.dto.CollectionResponse;
import com.lexify.mapper.CollectionMapper;
import com.lexify.model.Word;
import com.lexify.model.WordCollection;
import com.lexify.repository.CollectionRepository;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final CollectionRepository collections;
    private final UserRepository users;
    private final WordRepository words;
    private final CollectionMapper mapper;

    public List<CollectionResponse> list(Long userId) {
        return collections.findByUserIdOrderByIdDesc(userId).stream().map(mapper::toResponse).toList();
    }

    public CollectionResponse get(Long userId, Long id) {
        return mapper.toResponse(find(userId, id));
    }

    @Transactional
    public CollectionResponse create(Long userId, CollectionRequest request) {
        var collection = new WordCollection();
        collection.setUser(users.getReferenceById(userId));
        collection.setName(normalizeName(request.name()));
        collection.setDescription(request.description());
        return mapper.toResponse(collections.saveAndFlush(collection));
    }

    @Transactional
    public CollectionResponse update(Long userId, Long id, CollectionRequest request) {
        var collection = find(userId, id);
        collection.setName(normalizeName(request.name()));
        collection.setDescription(request.description());
        return mapper.toResponse(collections.saveAndFlush(collection));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        collections.delete(find(userId, id));
    }

    @Transactional
    public CollectionResponse addWord(Long userId, Long id, Long wordId) {
        var collection = find(userId, id);
        collection.getWords().add(findWord(userId, wordId));
        return mapper.toResponse(collections.saveAndFlush(collection));
    }

    @Transactional
    public void removeWord(Long userId, Long id, Long wordId) {
        var collection = find(userId, id);
        var word = findWord(userId, wordId);
        collection.getWords().remove(word);
        collections.saveAndFlush(collection);
    }

    private WordCollection find(Long userId, Long id) {
        return collections.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
    }

    private Word findWord(Long userId, Long id) {
        return words.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
    }

    private String normalizeName(String name) {
        var normalized = name.strip();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Collection name must not be blank");
        }
        return normalized;
    }
}
