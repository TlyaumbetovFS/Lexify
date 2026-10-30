package com.lexify.dto;

import java.time.Instant;

public record MessageResponse(Long id, Long roomId, Long authorId, String text, Instant createdAt) {
}
