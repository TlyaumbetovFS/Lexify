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
    private final DictionaryService dictionary;

    public List<WordResponse> list(Long userId) {
        return words.findByUserIdOrderByIdDesc(userId).stream().map(mapper::toResponse).toList();
    }

    public WordResponse get(Long userId, Long id) {
        return mapper.toResponse(find(userId, id));
    }

    @Transactional
    public WordResponse create(Long userId, WordRequest request) {
        var text = normalize(request.text());
        var source = detectLanguage(text);
        checkDuplicate(userId, text, source);
        var word = new Word();
        word.setUser(users.getReferenceById(userId));
        translate(word, text, source);
        fillDetails(word, request.example());
        return mapper.toResponse(words.saveAndFlush(word));
    }

    @Transactional
    public WordResponse update(Long userId, Long id, WordRequest request) {
        var word = find(userId, id);
        var text = normalize(request.text());
        var source = detectLanguage(text);
        var currentText = "ru".equals(source) ? word.getRussianText() : word.getEnglishText();
        if (!text.equalsIgnoreCase(currentText)) {
            checkDuplicate(userId, text, source);
            translate(word, text, source);
        }
        word.setSourceLanguage(source);
        fillDetails(word, request.example());
        return mapper.toResponse(words.saveAndFlush(word));
    }

    @Transactional
    public WordResponse enrich(Long userId, Long id) {
        var word = find(userId, id);
        fillDetails(word, word.getExample());
        return mapper.toResponse(words.saveAndFlush(word));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        words.delete(find(userId, id));
    }

    private void translate(Word word, String text, String source) {
        var target = "ru".equals(source) ? "en" : "ru";
        var translated = normalize(translation.translate(text, source, target));
        word.setRussianText("ru".equals(source) ? text : translated);
        word.setEnglishText("en".equals(source) ? text : translated);
        word.setSourceLanguage(source);
    }

    private void fillDetails(Word word, String example) {
        var entry = dictionary.lookup(word.getEnglishText());
        word.setDictionary(entry);
        word.setExample(example == null || example.isBlank() ? entry.getExample() : example.strip());
    }

    private Word find(Long userId, Long id) {
        return words.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
    }

    private void checkDuplicate(Long userId, String text, String source) {
        var exists = "ru".equals(source)
                ? words.existsByUserIdAndRussianTextIgnoreCase(userId, text)
                : words.existsByUserIdAndEnglishTextIgnoreCase(userId, text);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Word already in your vocabulary");
        }
    }

    private String detectLanguage(String text) {
        var russian = text.codePoints().anyMatch(code -> Character.UnicodeScript.of(code) == Character.UnicodeScript.CYRILLIC);
        var english = text.codePoints().anyMatch(code -> Character.UnicodeScript.of(code) == Character.UnicodeScript.LATIN);
        if (russian == english) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a word in Russian or English, without mixing languages");
        }
        return russian ? "ru" : "en";
    }

    private String normalize(String text) {
        var normalized = text.strip().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Word must not be blank");
        }
        return normalized;
    }
}
