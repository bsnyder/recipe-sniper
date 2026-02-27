package com.recipesniper.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailResponse(
        Long id,
        String url,
        String title,
        LocalDateTime createdAt,
        List<IngredientResponse> ingredients
) {
}
