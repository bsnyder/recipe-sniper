package com.recipesniper.dto;

import java.time.LocalDateTime;

public record ShoppingListResponse(
        Long id,
        String name,
        int recipeCount,
        int itemCount,
        LocalDateTime createdAt
) {
}
