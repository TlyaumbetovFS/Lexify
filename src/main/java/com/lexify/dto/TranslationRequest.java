package com.lexify.dto;

public record TranslationRequest(String q, String source, String target, String format) {
}
