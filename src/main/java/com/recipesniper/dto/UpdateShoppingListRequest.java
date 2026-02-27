package com.recipesniper.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateShoppingListRequest(
        @NotBlank String name,
        @NotNull List<@Valid ItemUpdate> items
) {
    public record ItemUpdate(
            Long id,
            @NotBlank String name,
            String quantity,
            String unit
    ) {
    }
}
