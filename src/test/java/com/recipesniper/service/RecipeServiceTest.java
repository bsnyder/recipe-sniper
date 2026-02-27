package com.recipesniper.service;

import com.recipesniper.dto.RecipeDetailResponse;
import com.recipesniper.dto.RecipeResponse;
import com.recipesniper.entity.Recipe;
import com.recipesniper.entity.RecipeIngredient;
import com.recipesniper.repository.RecipeRepository;
import com.recipesniper.service.IngredientExtractionService.ParsedIngredient;
import com.recipesniper.service.ScrapingService.ScrapeResult;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private ScrapingService scrapingService;

    @Mock
    private IngredientExtractionService extractionService;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(
                recipeRepository, scrapingService, extractionService,
                OpenTelemetry.noop().getTracer("test"));
    }

    @Test
    void shouldAddRecipeFromUrl() throws IOException {
        String url = "https://example.com/recipe";
        when(scrapingService.scrape(url))
                .thenReturn(new ScrapeResult("Chocolate Cake", "<html>cake</html>", Path.of("/tmp/test.html")));
        when(extractionService.extract(anyString()))
                .thenReturn(List.of(
                        new ParsedIngredient("flour", "2", "cups", "2 cups flour"),
                        new ParsedIngredient("sugar", "1", "cup", "1 cup sugar")
                ));

        Recipe savedRecipe = new Recipe();
        savedRecipe.setId(1L);
        savedRecipe.setUrl(url);
        savedRecipe.setTitle("Chocolate Cake");
        savedRecipe.setRawHtml("<html>cake</html>");
        savedRecipe.setCreatedAt(LocalDateTime.now());
        when(recipeRepository.save(any(Recipe.class))).thenReturn(savedRecipe);

        RecipeDetailResponse response = recipeService.addRecipe(url);

        assertThat(response.title()).isEqualTo("Chocolate Cake");
        assertThat(response.url()).isEqualTo(url);

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        Recipe captured = captor.getValue();
        assertThat(captured.getIngredients()).hasSize(2);
    }

    @Test
    void shouldGetAllRecipes() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setUrl("https://example.com/recipe");
        recipe.setTitle("Test Recipe");
        recipe.setCreatedAt(LocalDateTime.now());
        when(recipeRepository.findAll()).thenReturn(List.of(recipe));

        List<RecipeResponse> recipes = recipeService.getAllRecipes();

        assertThat(recipes).hasSize(1);
        assertThat(recipes.get(0).title()).isEqualTo("Test Recipe");
    }

    @Test
    void shouldGetRecipeById() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setUrl("https://example.com/recipe");
        recipe.setTitle("Test Recipe");
        recipe.setCreatedAt(LocalDateTime.now());

        RecipeIngredient ing = new RecipeIngredient();
        ing.setId(1L);
        ing.setName("flour");
        ing.setQuantity("2");
        ing.setUnit("cups");
        ing.setRawText("2 cups flour");
        ing.setRecipe(recipe);
        recipe.getIngredients().add(ing);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));

        RecipeDetailResponse response = recipeService.getRecipeById(1L);

        assertThat(response.title()).isEqualTo("Test Recipe");
        assertThat(response.ingredients()).hasSize(1);
        assertThat(response.ingredients().get(0).name()).isEqualTo("flour");
    }

    @Test
    void shouldSearchRecipesByTitle() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setUrl("https://example.com/cake");
        recipe.setTitle("Chocolate Cake");
        recipe.setCreatedAt(LocalDateTime.now());
        when(recipeRepository.findByTitleContainingIgnoreCase("choco")).thenReturn(List.of(recipe));

        List<RecipeResponse> results = recipeService.searchRecipes("choco");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("Chocolate Cake");
    }

    @Test
    void shouldThrowWhenRecipeNotFound() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getRecipeById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recipe not found");
    }

    @Test
    void shouldDeleteRecipe() {
        when(recipeRepository.existsById(1L)).thenReturn(true);

        recipeService.deleteRecipe(1L);

        verify(recipeRepository).deleteById(1L);
    }
}
