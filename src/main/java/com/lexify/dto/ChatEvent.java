package com.lexify.dto;

public record ChatEvent(String type, Long roomId, MessageResponse message) {
}
