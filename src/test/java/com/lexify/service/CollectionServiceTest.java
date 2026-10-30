package com.lexify.service;

import com.lexify.dto.CollectionRequest;
import com.lexify.dto.CollectionResponse;
import com.lexify.dto.WordResponse;
import com.lexify.mapper.CollectionMapper;
import com.lexify.model.User;
import com.lexify.model.Word;
import com.lexify.model.WordCollection;
import com.lexify.repository.CollectionRepository;
import com.lexify.repository.UserRepository;
import com.lexify.repository.WordRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CollectionRepository collections;
    @Mock
    private UserRepository users;
    @Mock
    private WordRepository words;
    private CollectionService service;

    @BeforeEach
    void setUp() {
        service = new CollectionService(collections, users, words, Mappers.getMapper(CollectionMapper.class));
    }

    @Test
    void createsCollectionWithOwnerAndTrimmedName() {
        var owner = new User();
        owner.setId(1L);
        when(users.getReferenceById(1L)).thenReturn(owner);
        when(collections.saveAndFlush(any())).thenAnswer(invocation -> {
            var collection = invocation.getArgument(0, WordCollection.class);
            collection.setId(10L);
            return collection;
        });

        var result = service.create(1L, new CollectionRequest(" Travel ", "Useful travel words"));

        assertThat(result).isEqualTo(new CollectionResponse(10L, "Travel", "Useful travel words", List.of()));
        var captor = ArgumentCaptor.forClass(WordCollection.class);
        verify(collections).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(owner);
    }

    @Test
    void listsOnlyOwnersCollections() {
        when(collections.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(collection()));
        assertThat(service.list(1L)).extracting(CollectionResponse::id).containsExactly(10L);
    }

    @Test
    void returnsCollectionWithWordDetails() {
        var collection = collection();
        collection.getWords().add(word());
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));

        var result = service.get(1L, 10L);

        assertThat(result.words()).containsExactly(new WordResponse(20L, "привет", "hello", null, "ru", null));
    }

    @Test
    void renamesCollectionWithoutChangingWords() {
        var collection = collection();
        var word = word();
        collection.getWords().add(word);
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));
        stubSave();

        var result = service.update(1L, 10L, new CollectionRequest(" Greetings ", null));

        assertThat(result.name()).isEqualTo("Greetings");
        assertThat(result.description()).isNull();
        assertThat(collection.getWords()).containsExactly(word);
    }

    @Test
    void addsOwnersWord() {
        var collection = collection();
        var word = word();
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));
        when(words.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(word));
        stubSave();

        var result = service.addWord(1L, 10L, 20L);

        assertThat(result.words()).extracting(WordResponse::id).containsExactly(20L);
        verify(words, never()).save(any());
    }

    @Test
    void addingSameWordTwiceDoesNotDuplicateIt() {
        var collection = collection();
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));
        when(words.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(word()));
        stubSave();

        service.addWord(1L, 10L, 20L);
        var result = service.addWord(1L, 10L, 20L);

        assertThat(result.words()).extracting(WordResponse::id).containsExactly(20L);
    }

    @Test
    void removesWordFromCollectionWithoutDeletingIt() {
        var collection = collection();
        var word = word();
        collection.getWords().add(word);
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));
        when(words.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(word));
        stubSave();

        service.removeWord(1L, 10L, 20L);

        assertThat(collection.getWords()).isEmpty();
        verify(collections).saveAndFlush(collection);
        verify(words, never()).delete(any());
    }

    @Test
    void removingWordNotInCollectionLeavesOtherWordsUntouched() {
        var collection = collection();
        var existing = word();
        collection.getWords().add(existing);
        var other = word();
        other.setId(21L);
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));
        when(words.findByIdAndUserId(21L, 1L)).thenReturn(Optional.of(other));
        stubSave();

        service.removeWord(1L, 10L, 21L);

        assertThat(collection.getWords()).containsExactly(existing);
    }

    @Test
    void deletesCollectionWithoutDeletingWords() {
        var collection = collection();
        collection.getWords().add(word());
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));

        service.delete(1L, 10L);

        verify(collections).delete(collection);
        verifyNoInteractions(words);
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "delete", "addWord", "removeWord"})
    void rejectsAccessToAnotherUsersCollection(String action) {
        assertStatus(404, () -> {
            switch (action) {
                case "get" -> service.get(2L, 10L);
                case "update" -> service.update(2L, 10L, new CollectionRequest("Travel", null));
                case "delete" -> service.delete(2L, 10L);
                case "addWord" -> service.addWord(2L, 10L, 20L);
                case "removeWord" -> service.removeWord(2L, 10L, 20L);
                default -> throw new IllegalArgumentException(action);
            }
        });
        verify(collections).findByIdAndUserId(10L, 2L);
        verify(collections, never()).saveAndFlush(any());
        verify(collections, never()).delete(any());
        verifyNoInteractions(words);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsAnotherUsersWord(boolean add) {
        var collection = collection();
        when(collections.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(collection));

        assertStatus(404, () -> {
            if (add) {
                service.addWord(1L, 10L, 30L);
            } else {
                service.removeWord(1L, 10L, 30L);
            }
        });

        assertThat(collection.getWords()).isEmpty();
        verify(words).findByIdAndUserId(30L, 1L);
        verify(collections, never()).saveAndFlush(any());
    }

    @Test
    void rejectsBlankName() {
        assertStatus(400, () -> service.create(1L, new CollectionRequest(" \t ", null)));
        verify(collections, never()).saveAndFlush(any());
    }

    private void stubSave() {
        when(collections.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private WordCollection collection() {
        var collection = new WordCollection();
        collection.setId(10L);
        collection.setName("Travel");
        return collection;
    }

    private Word word() {
        var word = new Word();
        word.setId(20L);
        word.setSourceLanguage("ru");
        word.setRussianText("привет");
        word.setEnglishText("hello");
        return word;
    }

    private void assertStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
    }
}
