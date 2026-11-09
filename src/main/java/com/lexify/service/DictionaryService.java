package com.lexify.service;

import com.lexify.client.DictionaryClient;
import com.lexify.dto.DictionaryData;
import com.lexify.model.DictionaryEntry;
import com.lexify.mapper.DictionaryMapper;
import com.lexify.repository.DictionaryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final DictionaryRepository entries;
    private final DictionaryClient client;
    private final DictionaryMapper mapper;

    public synchronized DictionaryEntry lookup(String englishText) {
        var key = englishText.strip().toLowerCase(Locale.ROOT);
        var cached = entries.findById(key);
        if (cached.isPresent()) {
            var entry = cached.get();
            var lifetime = entry.hasContent() ? Duration.ofDays(30) : Duration.ofMinutes(5);
            if (entry.getCheckedAt().plus(lifetime).isAfter(Instant.now())) {
                return entry;
            }
        }
        var result = client.lookup(key);
        if (result.isEmpty() && cached.isPresent()) {
            return cached.get();
        }
        var data = result.orElseGet(DictionaryData::empty);
        var entry = mapper.toEntry(data, key, Instant.now());
        return entries.saveAndFlush(entry);
    }
}
