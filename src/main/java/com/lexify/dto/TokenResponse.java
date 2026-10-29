package com.lexify.dto;



public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
