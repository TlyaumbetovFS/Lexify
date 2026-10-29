package com.lexify.client;

import com.lexify.dto.TranslationRequest;
import com.lexify.dto.TranslationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class TranslationClient {

    private final RestClient translationRestClient;

    public String translate(String text) {
        try {
            var response = translationRestClient.post()
                    .uri("/translate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TranslationRequest(text, "ru", "en", "text"))
                    .retrieve()
                    .body(TranslationResponse.class);
            if (response == null || response.translatedText() == null
                    || response.translatedText().isBlank() || response.translatedText().length() > 500) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid translation response");
            }
            return response.translatedText();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Translation service unavailable");
        }
    }
}
