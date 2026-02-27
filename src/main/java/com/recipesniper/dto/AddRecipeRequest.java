package com.recipesniper.dto;

import jakarta.validation.constraints.NotBlank;

public record AddRecipeRequest(
        @NotBlank(message = "URL is required")
        String url
) {
}
