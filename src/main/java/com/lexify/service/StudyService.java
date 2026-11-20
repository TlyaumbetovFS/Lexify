package com.lexify.service;

import com.lexify.dto.StudyRequest;
import com.lexify.dto.StudyResponse;
import com.lexify.mapper.WordMapper;
import com.lexify.model.StudyCard;
import com.lexify.model.StudyReview;
import com.lexify.repository.*;
import io.github.openspacedrepetition.Card;
import io.github.openspacedrepetition.Rating;
import io.github.openspacedrepetition.Scheduler;
import java.time.Instant;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {
    private final WordRepository words;
    private final CollectionRepository collections;
    private final StudyCardRepository cards;
    private final StudyReviewRepository reviews;
    private final WordMapper mapper;
    private final Scheduler scheduler = Scheduler.builder().enableFuzzing(false).build();

    public StudyResponse next(Long userId, String direction, Long collectionId) {
        if (!"en".equals(direction) && !"ru".equals(direction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid study direction");
        }
        if (collectionId != null && collections.findByIdAndUserId(collectionId, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }
        var now = Instant.now();
        var counts = words.studyCounts(userId, direction, collectionId, now).getFirst();
        var queue = words.studyQueue(userId, direction, collectionId, now, PageRequest.of(0, 1));
        StudyResponse.StudyItem item = null;
        if (!queue.isEmpty()) {
            var word = queue.getFirst();
            var saved = cards.findByWordIdAndDirection(word.getId(), direction).orElse(null);
            var state = saved == null ? fresh(now) : Card.fromJson(saved.getCardState());
            var options = Arrays.stream(Rating.values()).map(rating -> new StudyResponse.Option(
                    rating.getValue(), scheduler.reviewCard(state, rating, now, null).card().getDue())).toList();
            item = new StudyResponse.StudyItem(mapper.toResponse(word), direction,
                    saved == null ? 0 : saved.getReviews(), options);
        }
        return new StudyResponse(number(counts[0]), number(counts[1]), number(counts[2]),
                (Instant) counts[3], item);
    }

    @Transactional
    public void review(Long userId, StudyRequest request) {
        var word = words.lockOwned(userId, request.wordId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Word not found"));
        var now = Instant.now();
        var saved = cards.findByWordIdAndDirection(word.getId(), request.direction()).orElseGet(() -> {
            var card = new StudyCard();
            card.setWord(word);
            card.setDirection(request.direction());
            card.setDue(now);
            card.setCardState(fresh(now).toJson());
            return card;
        });
        if (saved.getReviews() != request.revision() || saved.getDue().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card already reviewed; refresh the queue");
        }
        var rating = Arrays.stream(Rating.values()).filter(value -> value.getValue() == request.rating())
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rating"));
        var result = scheduler.reviewCard(Card.fromJson(saved.getCardState()), rating, now, null).card();
        saved.setCardState(result.toJson());
        saved.setDue(result.getDue());
        saved.setReviews(saved.getReviews() + 1);
        cards.save(saved);
        var log = new StudyReview();
        log.setCard(saved);
        log.setRating(request.rating());
        log.setReviewedAt(now);
        log.setNextDue(result.getDue());
        reviews.save(log);
    }

    private Card fresh(Instant now) {
        return Card.builder().cardId(0).due(now).build();
    }

    private long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
