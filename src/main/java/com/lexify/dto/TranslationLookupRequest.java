package com.lexify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TranslationLookupRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z]+(?:['’-][A-Za-z]+)*") String text
) {
}
