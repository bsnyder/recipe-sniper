package com.recipesniper.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddRecipesToShoppingListRequest(
        @NotEmpty(message = "At least one recipe must be selected")
        List<Long> recipeIds
) {
}
