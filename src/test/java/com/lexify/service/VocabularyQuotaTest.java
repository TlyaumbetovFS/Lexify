package com.lexify.service;

import com.lexify.model.User;
import com.lexify.model.Word;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VocabularyQuotaTest {
    @Test
    void allowsExactWordLimitButRejectsAnotherWord() {
        var users = mock(UserRepository.class);
        var words = mock(WordRepository.class);
        var quota = new VocabularyQuota(users, words);
        when(users.lockById(1L)).thenReturn(Optional.of(new User()));
        when(words.countByUserId(1L)).thenReturn(9999L);
        when(words.textBytes(1L)).thenReturn(VocabularyQuota.MAX_TEXT_BYTES - 10);
        assertThat(quota.availableBytes(1L, 1)).isEqualTo(10);
        assertThatThrownBy(() -> quota.availableBytes(1L, 2)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void countsUtf8BytesAndRejectsOverflow() {
        var quota = new VocabularyQuota(null, null);
        var word = new Word();
        word.setEnglishText("hi");
        word.setRussianText("да");
        assertThat(VocabularyQuota.bytes(word)).isEqualTo(6);
        quota.checkSize(word, 6);
        assertThatThrownBy(() -> quota.checkSize(word, 5)).isInstanceOf(ResponseStatusException.class);
    }
}
