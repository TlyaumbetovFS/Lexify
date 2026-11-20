package com.lexify.dto;

import java.time.Instant;
import java.util.List;

public record StudyResponse(long total, long newCards, long dueCards, Instant nextDue, StudyItem card) {
    public record StudyItem(WordResponse word, String direction, int revision, List<Option> options) {}
    public record Option(int rating, Instant due) {}
}
