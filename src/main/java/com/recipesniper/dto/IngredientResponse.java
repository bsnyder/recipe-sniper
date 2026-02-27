package com.recipesniper.dto;

public record IngredientResponse(
        Long id,
        String name,
        String quantity,
        String unit,
        String rawText
) {
}
