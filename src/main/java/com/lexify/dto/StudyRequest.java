package com.lexify.dto;

import jakarta.validation.constraints.*;

public record StudyRequest(@NotNull @Positive Long wordId, @NotBlank @Pattern(regexp = "en|ru") String direction,
                           @Min(1) @Max(4) int rating, @Min(0) int revision) {
}
