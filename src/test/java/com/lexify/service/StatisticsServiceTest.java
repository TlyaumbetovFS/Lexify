package com.lexify.service;

import com.lexify.dto.StatisticsResponse;
import com.lexify.repository.MessageRepository;
import com.lexify.repository.RoomRepository;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final MessageRepository messages = mock(MessageRepository.class);
    private final WordRepository words = mock(WordRepository.class);
    private final StatisticsService service = new StatisticsService(users, rooms, messages, words);

    @Test
    void countsRecordsAndNormalizesUniqueWords() {
        var closed = mock(Runnable.class);
        when(users.streamIds()).thenReturn(Stream.of(1L, 2L).onClose(closed));
        when(rooms.streamIds()).thenReturn(Stream.of(1L).onClose(closed));
        when(messages.streamIds()).thenReturn(Stream.of(1L, 2L, 3L).onClose(closed));
        when(words.findEnglishTexts()).thenReturn(List.of("hello", " HELLO ", "book", "Book"));

        assertThat(service.get()).isEqualTo(new StatisticsResponse(2, 1, 3, 4, 2));
        verify(closed, times(3)).run();
    }

    @Test
    void emptyDatabaseHasZeroCounts() {
        when(users.streamIds()).thenReturn(Stream.empty());
        when(rooms.streamIds()).thenReturn(Stream.empty());
        when(messages.streamIds()).thenReturn(Stream.empty());
        when(words.findEnglishTexts()).thenReturn(List.of());

        assertThat(service.get()).isEqualTo(new StatisticsResponse(0, 0, 0, 0, 0));
    }
}
