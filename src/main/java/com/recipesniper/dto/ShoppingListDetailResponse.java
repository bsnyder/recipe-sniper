package com.recipesniper.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ShoppingListDetailResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        List<RecipeResponse> recipes,
        List<ShoppingListItemResponse> items
) {
}
