package com.lexify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordRequest(
        @NotBlank @Size(max = 100) String russianText,
        @Size(max = 1000) String example
) {
}
