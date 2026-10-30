package com.lexify.dto;

import java.util.List;

public record CollectionResponse(Long id, String name, String description, List<WordResponse> words) {
}
