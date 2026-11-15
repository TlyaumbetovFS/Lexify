package com.lexify.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, Long userId, String email) {
}
