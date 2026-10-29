package com.lexify.service;

import com.lexify.client.TranslationClient;
import com.lexify.dto.WordRequest;
import com.lexify.dto.WordResponse;
import com.lexify.mapper.WordMapper;
import com.lexify.model.User;
import com.lexify.model.Word;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock
    private WordRepository words;
    @Mock
    private UserRepository users;
    @Mock
    private TranslationClient translation;
    private WordService service;

    @BeforeEach
    void setUp() {
        service = new WordService(words, users, Mappers.getMapper(WordMapper.class), translation);
    }

    @Test
    void savesNormalizedWordWithTranslationAndOwner() {
        var owner = new User();
        owner.setId(1L);
        when(users.getReferenceById(1L)).thenReturn(owner);
        when(translation.translate("привет")).thenReturn("hello");
        when(words.saveAndFlush(any())).thenAnswer(invocation -> {
            var word = invocation.getArgument(0, Word.class);
            assertThat(word.getUser()).isSameAs(owner);
            word.setId(10L);
            return word;
        });

        var result = service.create(1L, new WordRequest(" ПрИвЕт ", "Hello, friend!"));

        assertThat(result).isEqualTo(new WordResponse(10L, "привет", "hello", "Hello, friend!"));
    }

    @Test
    void listsOnlyOwnersWords() {
        when(words.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(word()));
        assertThat(service.list(1L)).extracting(WordResponse::id).containsExactly(10L);
    }

    @Test
    void rejectsDuplicateBeforeRequestingTranslation() {
        when(words.existsByUserIdAndRussianText(1L, "привет")).thenReturn(true);
        assertStatus(409, () -> service.create(1L, new WordRequest("ПРИВЕТ", null)));
        verifyNoInteractions(translation);
        verify(words, never()).saveAndFlush(any());
    }

    @Test
    void doesNotSaveWhenTranslationFails() {
        when(translation.translate("привет")).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY));
        assertStatus(502, () -> service.create(1L, new WordRequest("привет", null)));
        verify(words, never()).saveAndFlush(any());
    }

    @Test
    void rejectsReadingAnotherUsersWord() {
        assertStatus(404, () -> service.get(2L, 10L));
        verify(words).findByIdAndUserId(10L, 2L);
    }

    @Test
    void rejectsUpdatingAnotherUsersWord() {
        assertStatus(404, () -> service.update(2L, 10L, new WordRequest("мир", null)));
        verifyNoInteractions(translation);
        verify(words, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDeletingAnotherUsersWord() {
        assertStatus(404, () -> service.delete(2L, 10L));
        verify(words, never()).delete(any());
    }

    @Test
    void updatesExampleWithoutRequestingTranslation() {
        when(words.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(word()));
        when(words.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(1L, 10L, new WordRequest("Привет", "Hello!"));

        assertThat(result.example()).isEqualTo("Hello!");
        assertThat(result.englishText()).isEqualTo("hello");
        verifyNoInteractions(translation);
    }

    @Test
    void translatesAgainWhenRussianTextChanges() {
        when(words.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(word()));
        when(translation.translate("мир")).thenReturn("world");
        when(words.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(1L, 10L, new WordRequest("мир", null));

        assertThat(result.russianText()).isEqualTo("мир");
        assertThat(result.englishText()).isEqualTo("world");
    }

    @Test
    void rejectsUpdatingToDuplicateWord() {
        when(words.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(word()));
        when(words.existsByUserIdAndRussianText(1L, "мир")).thenReturn(true);
        assertStatus(409, () -> service.update(1L, 10L, new WordRequest("мир", null)));
        verifyNoInteractions(translation);
    }

    @Test
    void deletesOwnersWord() {
        var word = word();
        when(words.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(word));
        service.delete(1L, 10L);
        verify(words).delete(word);
    }

    private Word word() {
        var word = new Word();
        word.setId(10L);
        word.setRussianText("привет");
        word.setEnglishText("hello");
        return word;
    }

    private void assertStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
    }
}
