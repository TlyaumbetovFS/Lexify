package com.lexify.service;

import com.lexify.client.TranslationClient;
import com.lexify.dto.WordRequest;
import com.lexify.dto.WordResponse;
import com.lexify.mapper.WordMapper;
import com.lexify.model.Word;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository words;
    private final UserRepository users;
    private final WordMapper mapper;
    private final TranslationClient translation;

    public List<WordResponse> list(Long userId) {
        return words.findByUserIdOrderByIdDesc(userId).stream().map(mapper::toResponse).toList();
    }

    public WordResponse get(Long userId, Long id) {
        return mapper.toResponse(find(userId, id));
    }

    @Transactional
    public WordResponse create(Long userId, WordRequest request) {
        var text = normalize(request.russianText());
        checkDuplicate(userId, text);
        var word = new Word();
        word.setUser(users.getReferenceById(userId));
        word.setRussianText(text);
        word.setEnglishText(translation.translate(text));
        word.setExample(request.example());
        return mapper.toResponse(words.saveAndFlush(word));
    }

    @Transactional
    public WordResponse update(Long userId, Long id, WordRequest request) {
        var word = find(userId, id);
        var text = normalize(request.russianText());
        if (!text.equals(word.getRussianText())) {
            checkDuplicate(userId, text);
            var translated = translation.translate(text);
            word.setRussianText(text);
            word.setEnglishText(translated);
        }
        word.setExample(request.example());
        return mapper.toResponse(words.saveAndFlush(word));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        words.delete(find(userId, id));
    }

    private Word find(Long userId, Long id) {
        return words.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
    }

    private void checkDuplicate(Long userId, String text) {
        if (words.existsByUserIdAndRussianText(userId, text)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Word already in your vocabulary");
        }
    }

    private String normalize(String text) {
        var normalized = text.strip().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word must not be blank");
        }
        return normalized;
    }
}
