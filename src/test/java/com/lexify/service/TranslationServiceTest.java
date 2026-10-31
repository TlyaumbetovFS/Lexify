package com.lexify.service;

import com.lexify.client.TranslationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationClient client;
    private TranslationService service;

    @BeforeEach
    void setUp() {
        service = new TranslationService(client);
    }

    @Test
    void translatesNormalizedEnglishWordToRussian() {
        when(client.translate("hello", "en", "ru")).thenReturn(" привет ");

        assertThat(service.translate("HELLO").translatedText()).isEqualTo("привет");
    }

    @Test
    void normalizesCurlyApostropheInContractions() {
        when(client.translate("don't", "en", "ru")).thenReturn("не");

        assertThat(service.translate("Don’t").translatedText()).isEqualTo("не");
    }

    @Test
    void preservesTranslationServiceFailure() {
        var error = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Translation service unavailable");
        when(client.translate("hello", "en", "ru")).thenThrow(error);

        assertThatThrownBy(() -> service.translate("hello")).isSameAs(error);
    }
}
