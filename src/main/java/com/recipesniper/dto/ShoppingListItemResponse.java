package com.recipesniper.dto;

public record ShoppingListItemResponse(
        Long id,
        String name,
        String quantity,
        String unit
) {
}
