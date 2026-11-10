package com.lexify.dto;

public record StatisticsResponse(long users, long rooms, long messages, long words, long uniqueWords) {
}
