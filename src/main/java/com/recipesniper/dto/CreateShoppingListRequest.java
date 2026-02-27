package com.recipesniper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateShoppingListRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotEmpty(message = "At least one recipe must be selected")
        List<Long> recipeIds
) {
}
