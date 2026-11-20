package com.lexify.service;

import com.lexify.dto.AnkiPreview;
import com.lexify.model.*;
import com.lexify.repository.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnkiImportServiceTest {
    @Mock AnkiPackageReader reader;
    @Mock UserRepository users;
    @Mock WordRepository words;
    @Mock CollectionRepository collections;
    AnkiImportService service;
    final MockMultipartFile file = new MockMultipartFile("file", new byte[1]);

    @BeforeEach
    void setup() {
        service = new AnkiImportService(reader, users, words, collections);
    }

    @Test
    void reusesExistingWordsAndReportsUnsupportedNotes() {
        var data = new AnkiPackageReader.PackageData(
                List.of(new AnkiPreview.NoteType(1, "Basic", List.of("EN", "RU", "Explanation"), 3, List.of())),
                List.of(new AnkiPackageReader.Note(1, 1, List.of("hello", "привет", "Неформальное приветствие"), false),
                        new AnkiPackageReader.Note(2, 1, List.of("world", "мир", "Земля и её обитатели"), false),
                        new AnkiPackageReader.Note(3, 1, List.of("world", "мир", "Земля и её обитатели"), false),
                        new AnkiPackageReader.Note(4, 2, List.of(), true)));
        when(reader.read(file)).thenReturn(data);
        when(users.lockById(1L)).thenReturn(Optional.of(new User()));
        var existing = new Word();
        existing.setEnglishText("hello");
        existing.setRussianText("привет");
        when(words.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(existing));
        when(collections.saveAndFlush(any())).thenAnswer(invocation -> {
            var collection = invocation.getArgument(0, WordCollection.class);
            assertThat(collection.getWords()).hasSize(2).contains(existing);
            collection.setId(5L);
            return collection;
        });
        var result = service.importWords(1L, file, 1, 0, 1, null, 2, "Anki");
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.reused()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.warnings()).hasSize(1);
        assertThat(existing.getRussianHint()).isEqualTo("Неформальное приветствие");
        verify(words).save(argThat(word -> "Земля и её обитатели".equals(word.getRussianHint())));
    }

    @Test
    void rejectsImportWhenVocabularyIsAtCapacity() {
        when(reader.read(file)).thenReturn(new AnkiPackageReader.PackageData(
                List.of(new AnkiPreview.NoteType(1, "Basic", List.of("EN", "RU"), 1, List.of())),
                List.of(new AnkiPackageReader.Note(1, 1, List.of("new", "новый"), false))));
        when(users.lockById(1L)).thenReturn(Optional.of(new User()));
        var word = new Word();
        word.setEnglishText("existing");
        word.setRussianText("старый");
        when(words.findByUserIdOrderByIdDesc(1L)).thenReturn(Collections.nCopies(10000, word));
        assertThatThrownBy(() -> service.importWords(1L, file, 1, 0, 1, null, null, "Anki"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Лимит словаря");
        verify(words, never()).save(any());
        verifyNoInteractions(collections);
    }

    @Test
    void rejectsBadMappingBeforeWriting() {
        when(reader.read(file)).thenReturn(new AnkiPackageReader.PackageData(
                List.of(new AnkiPreview.NoteType(1, "Basic", List.of("EN", "RU"), 1, List.of())), List.of()));
        assertThatThrownBy(() -> service.importWords(1L, file, 1, 0, 0, null, null, "Anki"))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(users, words, collections);
    }
}
