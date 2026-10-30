package com.lexify.service;

import com.lexify.client.DictionaryClient;
import com.lexify.dto.DictionaryData;
import com.lexify.model.DictionaryEntry;
import com.lexify.repository.DictionaryRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock
    private DictionaryRepository entries;
    @Mock
    private DictionaryClient client;
    private DictionaryService service;

    @BeforeEach
    void setUp() {
        service = new DictionaryService(entries, client);
    }

    @Test
    void reusesNormalizedSharedCacheWithoutExternalCall() {
        var entry = entry();
        when(entries.findById("hello")).thenReturn(Optional.of(entry));

        assertThat(service.lookup(" HELLO ")).isSameAs(entry);

        verifyNoInteractions(client);
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void storesSuccessfulLookup() {
        when(client.lookup("hello")).thenReturn(Optional.of(data()));
        stubSave();

        var result = service.lookup("Hello");

        assertThat(result.getWord()).isEqualTo("hello");
        assertThat(result.getExample()).isEqualTo("Hello, everyone!");
        assertThat(result.getPhonetic()).isEqualTo("/hello/");
        assertThat(result.getAudioUrl()).isEqualTo("https://example.com/hello.mp3");
        assertThat(result.getCheckedAt()).isNotNull();
    }

    @Test
    void temporarilyCachesMissingWord() {
        var entry = entry();
        entry.setExample(null);
        when(entries.findById("hello")).thenReturn(Optional.of(entry));

        assertThat(service.lookup("hello")).isSameAs(entry);

        verifyNoInteractions(client);
    }

    @Test
    void retriesExpiredMissingWord() {
        var entry = entry();
        entry.setExample(null);
        entry.setCheckedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        when(entries.findById("hello")).thenReturn(Optional.of(entry));
        when(client.lookup("hello")).thenReturn(Optional.of(data()));
        stubSave();

        assertThat(service.lookup("hello").getExample()).isEqualTo("Hello, everyone!");
    }

    @Test
    void keepsStaleSuccessfulDetailsDuringProviderOutage() {
        var entry = entry();
        entry.setCheckedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        when(entries.findById("hello")).thenReturn(Optional.of(entry));
        when(client.lookup("hello")).thenReturn(Optional.empty());

        assertThat(service.lookup("hello")).isSameAs(entry);
        assertThat(entry.getExample()).isEqualTo("Hello, everyone!");
        verify(entries, never()).saveAndFlush(any());
    }

    @Test
    void storesEmptyShortLivedCacheDuringOutageWithoutExistingDetails() {
        when(client.lookup("hello")).thenReturn(Optional.empty());
        stubSave();
        var result = service.lookup("hello");
        assertThat(result.hasContent()).isFalse();
        assertThat(result.getCheckedAt()).isNotNull();
    }

    @Test
    void concurrentRequestsForSameWordShareOneLookup() throws Exception {
        var stored = new AtomicReference<DictionaryEntry>();
        when(entries.findById("hello")).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(entries.saveAndFlush(any())).thenAnswer(invocation -> {
            var entry = invocation.getArgument(0, DictionaryEntry.class);
            stored.set(entry);
            return entry;
        });
        when(client.lookup("hello")).thenReturn(Optional.of(data()));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.lookup("hello"));
            var second = executor.submit(() -> service.lookup("HELLO"));
            assertThat(first.get()).isSameAs(second.get());
        }
        verify(client, times(1)).lookup("hello");
    }

    private void stubSave() {
        when(entries.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private DictionaryEntry entry() {
        var entry = new DictionaryEntry();
        entry.setWord("hello");
        entry.setExample("Hello, everyone!");
        entry.setCheckedAt(Instant.now());
        return entry;
    }

    private DictionaryData data() {
        return new DictionaryData("Hello, everyone!", "/hello/", "https://example.com/hello.mp3",
                "https://en.wiktionary.org/wiki/hello", null, null, null);
    }
}
