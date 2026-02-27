package com.recipesniper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipesniper.dto.AddRecipeRequest;
import com.recipesniper.dto.IngredientResponse;
import com.recipesniper.dto.RecipeDetailResponse;
import com.recipesniper.dto.RecipeResponse;
import com.recipesniper.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RecipeService recipeService;

    @Test
    void shouldAddRecipe() throws Exception {
        var request = new AddRecipeRequest("https://example.com/recipe");
        var response = new RecipeDetailResponse(
                1L, "https://example.com/recipe", "Chocolate Cake",
                LocalDateTime.now(),
                List.of(new IngredientResponse(1L, "flour", "2", "cups", "2 cups flour")));

        when(recipeService.addRecipe("https://example.com/recipe")).thenReturn(response);

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Chocolate Cake"))
                .andExpect(jsonPath("$.ingredients[0].name").value("flour"));
    }

    @Test
    void shouldReturnBadRequestForBlankUrl() throws Exception {
        var request = new AddRecipeRequest("");

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAllRecipes() throws Exception {
        var recipes = List.of(
                new RecipeResponse(1L, "https://example.com/r1", "Recipe 1", 3, LocalDateTime.now()),
                new RecipeResponse(2L, "https://example.com/r2", "Recipe 2", 5, LocalDateTime.now()));
        when(recipeService.getAllRecipes()).thenReturn(recipes);

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Recipe 1"));
    }

    @Test
    void shouldSearchRecipes() throws Exception {
        var recipes = List.of(
                new RecipeResponse(1L, "https://example.com/cake", "Chocolate Cake", 3, LocalDateTime.now()));
        when(recipeService.searchRecipes("choco")).thenReturn(recipes);

        mockMvc.perform(get("/api/recipes").param("search", "choco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Chocolate Cake"));
    }

    @Test
    void shouldGetRecipeById() throws Exception {
        var response = new RecipeDetailResponse(
                1L, "https://example.com/recipe", "Chocolate Cake",
                LocalDateTime.now(),
                List.of(new IngredientResponse(1L, "flour", "2", "cups", "2 cups flour")));
        when(recipeService.getRecipeById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Chocolate Cake"))
                .andExpect(jsonPath("$.ingredients.length()").value(1));
    }

    @Test
    void shouldReturn404WhenRecipeNotFound() throws Exception {
        when(recipeService.getRecipeById(99L))
                .thenThrow(new IllegalArgumentException("Recipe not found: 99"));

        mockMvc.perform(get("/api/recipes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteRecipe() throws Exception {
        mockMvc.perform(delete("/api/recipes/1"))
                .andExpect(status().isNoContent());

        verify(recipeService).deleteRecipe(1L);
    }
}
