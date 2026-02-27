package com.recipesniper.repository;

import com.recipesniper.entity.Recipe;
import com.recipesniper.entity.RecipeIngredient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecipeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void shouldSaveAndFindRecipe() {
        Recipe recipe = new Recipe();
        recipe.setUrl("https://example.com/recipe");
        recipe.setTitle("Test Recipe");
        recipe.setRawHtml("<html>test</html>");

        Recipe saved = recipeRepository.save(recipe);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Recipe> found = recipeRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Recipe");
        assertThat(found.get().getUrl()).isEqualTo("https://example.com/recipe");
    }

    @Test
    void shouldSaveRecipeWithIngredients() {
        Recipe recipe = new Recipe();
        recipe.setUrl("https://example.com/recipe");
        recipe.setTitle("Test Recipe");
        recipe.setRawHtml("<html>test</html>");

        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setName("flour");
        ingredient.setQuantity("2");
        ingredient.setUnit("cups");
        ingredient.setRawText("2 cups flour");
        ingredient.setRecipe(recipe);
        recipe.getIngredients().add(ingredient);

        Recipe saved = recipeRepository.save(recipe);
        entityManager.flush();
        entityManager.clear();

        Recipe found = recipeRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getIngredients()).hasSize(1);
        assertThat(found.getIngredients().get(0).getName()).isEqualTo("flour");
        assertThat(found.getIngredients().get(0).getQuantity()).isEqualTo("2");
        assertThat(found.getIngredients().get(0).getUnit()).isEqualTo("cups");
    }

    @Test
    void shouldCascadeDeleteIngredients() {
        Recipe recipe = new Recipe();
        recipe.setUrl("https://example.com/recipe");
        recipe.setTitle("Test Recipe");
        recipe.setRawHtml("<html>test</html>");

        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setName("sugar");
        ingredient.setQuantity("1");
        ingredient.setUnit("cup");
        ingredient.setRawText("1 cup sugar");
        ingredient.setRecipe(recipe);
        recipe.getIngredients().add(ingredient);

        Recipe saved = recipeRepository.save(recipe);
        entityManager.flush();

        recipeRepository.deleteById(saved.getId());
        entityManager.flush();

        assertThat(recipeRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void shouldFindAllRecipes() {
        Recipe recipe1 = new Recipe();
        recipe1.setUrl("https://example.com/recipe1");
        recipe1.setTitle("Recipe 1");
        recipe1.setRawHtml("<html>1</html>");

        Recipe recipe2 = new Recipe();
        recipe2.setUrl("https://example.com/recipe2");
        recipe2.setTitle("Recipe 2");
        recipe2.setRawHtml("<html>2</html>");

        recipeRepository.save(recipe1);
        recipeRepository.save(recipe2);

        List<Recipe> all = recipeRepository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void shouldSearchByTitleCaseInsensitive() {
        Recipe cake = new Recipe();
        cake.setUrl("https://example.com/cake");
        cake.setTitle("Chocolate Cake");
        cake.setRawHtml("<html>cake</html>");

        Recipe bread = new Recipe();
        bread.setUrl("https://example.com/bread");
        bread.setTitle("Banana Bread");
        bread.setRawHtml("<html>bread</html>");

        recipeRepository.save(cake);
        recipeRepository.save(bread);

        List<Recipe> results = recipeRepository.findByTitleContainingIgnoreCase("choco");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Chocolate Cake");

        assertThat(recipeRepository.findByTitleContainingIgnoreCase("bread")).hasSize(1);
        assertThat(recipeRepository.findByTitleContainingIgnoreCase("BANANA")).hasSize(1);
        assertThat(recipeRepository.findByTitleContainingIgnoreCase("pizza")).isEmpty();
    }

    @Test
    void shouldDeleteRecipeById() {
        Recipe recipe = new Recipe();
        recipe.setUrl("https://example.com/recipe");
        recipe.setTitle("Test Recipe");
        recipe.setRawHtml("<html>test</html>");

        Recipe saved = recipeRepository.save(recipe);
        recipeRepository.deleteById(saved.getId());

        assertThat(recipeRepository.findById(saved.getId())).isEmpty();
    }
}
