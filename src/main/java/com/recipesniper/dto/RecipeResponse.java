package com.recipesniper.dto;

import java.time.LocalDateTime;

public record RecipeResponse(
        Long id,
        String url,
        String title,
        int ingredientCount,
        LocalDateTime createdAt
) {
}
