package com.lexify.service;

import com.lexify.dto.StudyRequest;
import com.lexify.mapper.WordMapper;
import com.lexify.model.*;
import com.lexify.repository.*;
import io.github.openspacedrepetition.Card;
import io.github.openspacedrepetition.State;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {
    @Mock WordRepository words;
    @Mock CollectionRepository collections;
    @Mock StudyCardRepository cards;
    @Mock StudyReviewRepository reviews;
    @Mock WordMapper mapper;
    StudyService service;
    Word word;

    @BeforeEach
    void setup() {
        service = new StudyService(words, collections, cards, reviews, mapper);
        word = new Word();
        word.setId(7L);
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    void schedulesAndStoresEachRating(int rating) {
        when(words.lockOwned(1L, 7L)).thenReturn(Optional.of(word));
        when(cards.findByWordIdAndDirection(7L, "en")).thenReturn(Optional.empty());
        service.review(1L, new StudyRequest(7L, "en", rating, 0));
        verify(cards).save(argThat(saved -> {
            var card = Card.fromJson(saved.getCardState());
            assertThat(card.getStability()).isPositive();
            assertThat(saved.getReviews()).isEqualTo(1);
            assertThat(saved.getDue()).isAfter(Instant.now());
            assertThat(card.getState()).isEqualTo(rating == 4 ? State.REVIEW : State.LEARNING);
            return saved.getWord() == word && saved.getDirection().equals("en");
        }));
        verify(reviews).save(argThat(log -> log.getRating() == rating && log.getNextDue().isAfter(log.getReviewedAt())));
    }

    @Test
    void rejectsOtherUsersWord() {
        when(words.lockOwned(2L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.review(2L, new StudyRequest(7L, "en", 3, 0)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(cards, reviews);
    }

    @Test
    void rejectsRepeatedSubmissionAndEarlyReview() {
        when(words.lockOwned(1L, 7L)).thenReturn(Optional.of(word));
        var saved = new StudyCard();
        saved.setReviews(1);
        saved.setDue(Instant.now().plusSeconds(600));
        when(cards.findByWordIdAndDirection(7L, "en")).thenReturn(Optional.of(saved));
        assertThatThrownBy(() -> service.review(1L, new StudyRequest(7L, "en", 3, 0)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThatThrownBy(() -> service.review(1L, new StudyRequest(7L, "en", 3, 1)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verifyNoInteractions(reviews);
    }

    @Test
    void rejectsInvalidDirectionAndForeignCollection() {
        assertThatThrownBy(() -> service.next(1L, "de", null)).isInstanceOf(ResponseStatusException.class);
        when(collections.findByIdAndUserId(4L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.next(1L, "en", 4L)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(words);
    }
}
