package com.lexify.service;

import com.lexify.client.TranslationClient;
import com.lexify.dto.TranslationResponse;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationClient client;

    public TranslationResponse translate(String text) {
        var word = text.strip().toLowerCase(Locale.ROOT).replace('’', '\'');
        return new TranslationResponse(client.translate(word, "en", "ru").strip());
    }
}
