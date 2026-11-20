package com.lexify.service;

import com.lexify.model.Word;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VocabularyQuota {
    public static final int MAX_WORDS = 10000;
    public static final long MAX_TEXT_BYTES = 30L * 1024 * 1024;
    private final UserRepository users;
    private final WordRepository words;

    public long availableBytes(Long userId, int additionalWords) {
        users.lockById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (words.countByUserId(userId) + additionalWords > MAX_WORDS) throw exceeded();
        return MAX_TEXT_BYTES - words.textBytes(userId);
    }

    public void checkSize(Word word, long availableBytes) {
        if (bytes(word) > availableBytes) throw exceeded();
    }

    public static long bytes(Word word) {
        long total = 0;
        for (var field : new String[] {word.getEnglishText(), word.getRussianText(), word.getExample()}) {
            if (field != null) total += field.getBytes(StandardCharsets.UTF_8).length;
        }
        return total;
    }

    private ResponseStatusException exceeded() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Лимит словаря: 10 000 слов и 30 МБ текста на пользователя.");
    }
}
