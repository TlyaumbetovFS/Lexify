package com.lexify.dto;

import java.util.List;

public record RoomResponse(Long id, String name, String description, Long ownerId, List<Long> moderatorIds) {
}
