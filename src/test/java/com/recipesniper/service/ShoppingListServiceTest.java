package com.recipesniper.service;

import com.recipesniper.dto.ShoppingListDetailResponse;
import com.recipesniper.dto.ShoppingListResponse;
import com.recipesniper.dto.UpdateShoppingListRequest;
import com.recipesniper.entity.Recipe;
import com.recipesniper.entity.RecipeIngredient;
import com.recipesniper.entity.ShoppingList;
import com.recipesniper.entity.ShoppingListItem;
import com.recipesniper.repository.RecipeRepository;
import com.recipesniper.repository.ShoppingListRepository;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private RecipeRepository recipeRepository;

    private ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        shoppingListService = new ShoppingListService(
                shoppingListRepository, recipeRepository,
                OpenTelemetry.noop().getTracer("test"));
    }

    @Test
    void shouldCreateShoppingListFromRecipes() {
        Recipe recipe = createRecipeWithIngredients(1L, "Cake",
                List.of(ingredient("flour", "2", "cups"),
                        ingredient("sugar", "1", "cup")));
        when(recipeRepository.findAllById(List.of(1L))).thenReturn(List.of(recipe));

        ShoppingList savedList = new ShoppingList();
        savedList.setId(1L);
        savedList.setName("Weekly");
        savedList.setCreatedAt(LocalDateTime.now());
        savedList.setRecipes(List.of(recipe));
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(savedList);

        ShoppingListDetailResponse response =
                shoppingListService.createShoppingList("Weekly", List.of(1L));

        assertThat(response.name()).isEqualTo("Weekly");

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        assertThat(captured.getItems()).hasSize(2);
        assertThat(captured.getRecipes()).hasSize(1);
    }

    @Test
    void shouldCombineDuplicateIngredientsFromMultipleRecipes() {
        Recipe recipe1 = createRecipeWithIngredients(1L, "Cake",
                List.of(ingredient("flour", "2", "cups")));
        Recipe recipe2 = createRecipeWithIngredients(2L, "Bread",
                List.of(ingredient("flour", "3", "cups"),
                        ingredient("yeast", "1", "packet")));
        when(recipeRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(recipe1, recipe2));

        ShoppingList savedList = new ShoppingList();
        savedList.setId(1L);
        savedList.setName("Combined");
        savedList.setCreatedAt(LocalDateTime.now());
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(savedList);

        shoppingListService.createShoppingList("Combined", List.of(1L, 2L));

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        // flour (2+3=5 cups) combined, yeast separate => 2 items
        assertThat(captured.getItems()).hasSize(2);
        ShoppingListItem flourItem = captured.getItems().stream()
                .filter(i -> i.getName().equals("flour")).findFirst().orElseThrow();
        assertThat(flourItem.getQuantity()).isEqualTo("5");
        assertThat(flourItem.getUnit()).isEqualTo("cups");
    }

    @Test
    void shouldCombineItemsWithDifferentUnitsIntoDescription() {
        Recipe recipe = createRecipeWithIngredients(1L, "Mixed",
                List.of(ingredient("butter", "2", "tbsp"),
                        ingredient("butter", "1", "cup")));
        when(recipeRepository.findAllById(List.of(1L))).thenReturn(List.of(recipe));

        ShoppingList savedList = new ShoppingList();
        savedList.setId(1L);
        savedList.setName("Test");
        savedList.setCreatedAt(LocalDateTime.now());
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(savedList);

        shoppingListService.createShoppingList("Test", List.of(1L));

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        // Different units => combined into single item with descriptive quantity
        assertThat(captured.getItems()).hasSize(1);
        ShoppingListItem butter = captured.getItems().get(0);
        assertThat(butter.getName()).isEqualTo("butter");
        assertThat(butter.getQuantity()).isEqualTo("2 tbsp + 1 cup");
        assertThat(butter.getUnit()).isNull();
    }

    @Test
    void shouldCombineItemsWhenOneHasNoUnit() {
        Recipe recipe1 = createRecipeWithIngredients(1L, "Recipe A",
                List.of(ingredient("olive oil", "1/4", "cup")));
        Recipe recipe2 = createRecipeWithIngredients(2L, "Recipe B",
                List.of(ingredientNoUnit("olive oil")));
        when(recipeRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(recipe1, recipe2));

        ShoppingList savedList = new ShoppingList();
        savedList.setId(1L);
        savedList.setName("Test");
        savedList.setCreatedAt(LocalDateTime.now());
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(savedList);

        shoppingListService.createShoppingList("Test", List.of(1L, 2L));

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        // Same name, one has unit, other doesn't => combined
        assertThat(captured.getItems()).hasSize(1);
        ShoppingListItem oil = captured.getItems().get(0);
        assertThat(oil.getName()).isEqualTo("olive oil");
        assertThat(oil.getUnit()).isEqualTo("cup");
    }

    @Test
    void shouldGetAllShoppingLists() {
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        list.setName("Weekly");
        list.setCreatedAt(LocalDateTime.now());
        when(shoppingListRepository.findAll()).thenReturn(List.of(list));

        List<ShoppingListResponse> lists = shoppingListService.getAllShoppingLists();

        assertThat(lists).hasSize(1);
        assertThat(lists.get(0).name()).isEqualTo("Weekly");
    }

    @Test
    void shouldGetShoppingListById() {
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        list.setName("Weekly");
        list.setCreatedAt(LocalDateTime.now());

        ShoppingListItem item = new ShoppingListItem();
        item.setId(1L);
        item.setName("flour");
        item.setQuantity("2");
        item.setUnit("cups");
        item.setShoppingList(list);
        list.getItems().add(item);

        when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(list));

        ShoppingListDetailResponse response = shoppingListService.getShoppingListById(1L);

        assertThat(response.name()).isEqualTo("Weekly");
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void shouldThrowWhenShoppingListNotFound() {
        when(shoppingListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingListService.getShoppingListById(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDeleteShoppingList() {
        when(shoppingListRepository.existsById(1L)).thenReturn(true);

        shoppingListService.deleteShoppingList(1L);

        verify(shoppingListRepository).deleteById(1L);
    }

    @Test
    void shouldUpdateShoppingList() {
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        list.setName("Weekly");
        list.setCreatedAt(LocalDateTime.now());

        ShoppingListItem existingItem = new ShoppingListItem();
        existingItem.setId(1L);
        existingItem.setName("flour");
        existingItem.setQuantity("2");
        existingItem.setUnit("cups");
        existingItem.setShoppingList(list);
        list.getItems().add(existingItem);

        when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(list));
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(list);

        var request = new UpdateShoppingListRequest("Updated Weekly", List.of(
                new UpdateShoppingListRequest.ItemUpdate(null, "butter", "1", "stick"),
                new UpdateShoppingListRequest.ItemUpdate(null, "eggs", "3", null)
        ));

        ShoppingListDetailResponse response = shoppingListService.updateShoppingList(1L, request);

        assertThat(response.name()).isEqualTo("Updated Weekly");

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo("Updated Weekly");
        assertThat(captured.getItems()).hasSize(2);
        assertThat(captured.getItems().get(0).getName()).isEqualTo("butter");
        assertThat(captured.getItems().get(1).getName()).isEqualTo("eggs");
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentList() {
        when(shoppingListRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new UpdateShoppingListRequest("Name", List.of());
        assertThatThrownBy(() -> shoppingListService.updateShoppingList(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Shopping list not found");
    }

    @Test
    void shouldAddRecipesToExistingShoppingList() {
        // Existing list with one recipe and one item
        Recipe existingRecipe = createRecipeWithIngredients(1L, "Cake",
                List.of(ingredient("flour", "2", "cups")));
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        list.setName("Weekly");
        list.setCreatedAt(LocalDateTime.now());
        list.setRecipes(new java.util.ArrayList<>(List.of(existingRecipe)));
        ShoppingListItem existingItem = new ShoppingListItem();
        existingItem.setId(1L);
        existingItem.setName("flour");
        existingItem.setQuantity("2");
        existingItem.setUnit("cups");
        existingItem.setShoppingList(list);
        list.getItems().add(existingItem);

        // New recipe to add
        Recipe newRecipe = createRecipeWithIngredients(2L, "Bread",
                List.of(ingredient("yeast", "1", "packet")));

        when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(list));
        when(recipeRepository.findAllById(List.of(2L))).thenReturn(List.of(newRecipe));
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(list);

        ShoppingListDetailResponse response =
                shoppingListService.addRecipesToShoppingList(1L, List.of(2L));

        assertThat(response.name()).isEqualTo("Weekly");

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        assertThat(captured.getRecipes()).hasSize(2);
        assertThat(captured.getItems()).hasSize(2); // flour + yeast (different names)
    }

    @Test
    void shouldCombineDuplicatesWhenAddingRecipesWithSameIngredient() {
        // Existing list with flour 2 cups
        Recipe existingRecipe = createRecipeWithIngredients(1L, "Cake",
                List.of(ingredient("flour", "2", "cups")));
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        list.setName("Weekly");
        list.setCreatedAt(LocalDateTime.now());
        list.setRecipes(new java.util.ArrayList<>(List.of(existingRecipe)));
        ShoppingListItem existingItem = new ShoppingListItem();
        existingItem.setName("flour");
        existingItem.setQuantity("2");
        existingItem.setUnit("cups");
        existingItem.setShoppingList(list);
        list.getItems().add(existingItem);

        // New recipe also has flour 3 cups
        Recipe newRecipe = createRecipeWithIngredients(2L, "Bread",
                List.of(ingredient("flour", "3", "cups")));

        when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(list));
        when(recipeRepository.findAllById(List.of(2L))).thenReturn(List.of(newRecipe));
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(list);

        shoppingListService.addRecipesToShoppingList(1L, List.of(2L));

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        assertThat(captured.getItems()).hasSize(1); // flour combined
        assertThat(captured.getItems().get(0).getQuantity()).isEqualTo("5");
    }

    @Test
    void shouldNotDuplicateRecipeWhenAddingExistingRecipe() {
        Recipe recipe = createRecipeWithIngredients(1L, "Cake",
                List.of(ingredient("flour", "2", "cups")));
        ShoppingList list = new ShoppingList();
        list.setId(1L);
        list.setName("Weekly");
        list.setCreatedAt(LocalDateTime.now());
        list.setRecipes(new java.util.ArrayList<>(List.of(recipe)));
        ShoppingListItem existingItem = new ShoppingListItem();
        existingItem.setName("flour");
        existingItem.setQuantity("2");
        existingItem.setUnit("cups");
        existingItem.setShoppingList(list);
        list.getItems().add(existingItem);

        when(shoppingListRepository.findById(1L)).thenReturn(Optional.of(list));
        when(recipeRepository.findAllById(List.of(1L))).thenReturn(List.of(recipe));
        when(shoppingListRepository.save(any(ShoppingList.class))).thenReturn(list);

        shoppingListService.addRecipesToShoppingList(1L, List.of(1L));

        ArgumentCaptor<ShoppingList> captor = ArgumentCaptor.forClass(ShoppingList.class);
        verify(shoppingListRepository).save(captor.capture());
        ShoppingList captured = captor.getValue();
        // Recipe not duplicated in the recipes list
        assertThat(captured.getRecipes()).hasSize(1);
        // Flour combined: 2 + 2 = 4
        assertThat(captured.getItems()).hasSize(1);
        assertThat(captured.getItems().get(0).getQuantity()).isEqualTo("4");
    }

    @Test
    void shouldThrowWhenRecipeNotFound() {
        when(recipeRepository.findAllById(List.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> shoppingListService.createShoppingList("Test", List.of(99L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- helpers ---

    private Recipe createRecipeWithIngredients(Long id, String title,
                                               List<RecipeIngredient> ingredients) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setUrl("https://example.com/" + title.toLowerCase());
        recipe.setCreatedAt(LocalDateTime.now());
        for (RecipeIngredient ing : ingredients) {
            ing.setRecipe(recipe);
            recipe.getIngredients().add(ing);
        }
        return recipe;
    }

    private RecipeIngredient ingredient(String name, String qty, String unit) {
        RecipeIngredient ing = new RecipeIngredient();
        ing.setName(name);
        ing.setQuantity(qty);
        ing.setUnit(unit);
        ing.setRawText(qty + " " + unit + " " + name);
        return ing;
    }

    private RecipeIngredient ingredientNoUnit(String name) {
        RecipeIngredient ing = new RecipeIngredient();
        ing.setName(name);
        ing.setRawText(name);
        return ing;
    }
}
