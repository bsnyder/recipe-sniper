package com.recipesniper.service;

import com.recipesniper.dto.RecipeResponse;
import com.recipesniper.dto.ShoppingListDetailResponse;
import com.recipesniper.dto.ShoppingListItemResponse;
import com.recipesniper.dto.ShoppingListResponse;
import com.recipesniper.dto.UpdateShoppingListRequest;
import com.recipesniper.entity.Recipe;
import com.recipesniper.entity.RecipeIngredient;
import com.recipesniper.entity.ShoppingList;
import com.recipesniper.entity.ShoppingListItem;
import com.recipesniper.repository.RecipeRepository;
import com.recipesniper.repository.ShoppingListRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShoppingListService {

    private static final Logger log = LoggerFactory.getLogger(ShoppingListService.class);

    private final ShoppingListRepository shoppingListRepository;
    private final RecipeRepository recipeRepository;
    private final Tracer tracer;

    public ShoppingListService(ShoppingListRepository shoppingListRepository,
                               RecipeRepository recipeRepository,
                               Tracer tracer) {
        this.shoppingListRepository = shoppingListRepository;
        this.recipeRepository = recipeRepository;
        this.tracer = tracer;
    }

    @Transactional
    public ShoppingListDetailResponse createShoppingList(String name, List<Long> recipeIds) {
        Span span = tracer.spanBuilder("ShoppingListService.createShoppingList")
                .setAttribute("shoppingList.name", name)
                .setAttribute("shoppingList.recipeCount", (long) recipeIds.size())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.info("Creating shopping list '{}' from {} recipes", name, recipeIds.size());

            List<Recipe> recipes = recipeRepository.findAllById(recipeIds);
            if (recipes.isEmpty()) {
                throw new IllegalArgumentException("No valid recipes found for IDs: " + recipeIds);
            }
            if (recipes.size() != recipeIds.size()) {
                log.warn("Some recipe IDs not found. Requested: {}, Found: {}",
                        recipeIds.size(), recipes.size());
            }

            ShoppingList shoppingList = new ShoppingList();
            shoppingList.setName(name);
            shoppingList.setRecipes(recipes);

            // Collect all ingredients then combine duplicates
            List<ShoppingListItem> rawItems = new ArrayList<>();
            for (Recipe recipe : recipes) {
                for (RecipeIngredient ri : recipe.getIngredients()) {
                    ShoppingListItem item = new ShoppingListItem();
                    item.setName(ri.getName());
                    item.setQuantity(ri.getQuantity());
                    item.setUnit(ri.getUnit());
                    rawItems.add(item);
                }
            }
            for (ShoppingListItem combined : combineItems(rawItems)) {
                combined.setShoppingList(shoppingList);
                shoppingList.getItems().add(combined);
            }

            ShoppingList saved = shoppingListRepository.save(shoppingList);
            span.setAttribute("shoppingList.id", saved.getId());
            span.setAttribute("shoppingList.itemCount", saved.getItems().size());
            log.info("Created shopping list '{}' with {} items", saved.getName(), saved.getItems().size());

            return toDetailResponse(saved);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public List<ShoppingListResponse> getAllShoppingLists() {
        return shoppingListRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShoppingListDetailResponse getShoppingListById(Long id) {
        ShoppingList list = shoppingListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shopping list not found: " + id));
        return toDetailResponse(list);
    }

    @Transactional
    public ShoppingListDetailResponse updateShoppingList(Long id, UpdateShoppingListRequest request) {
        Span span = tracer.spanBuilder("ShoppingListService.updateShoppingList")
                .setAttribute("shoppingList.id", id)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.info("Updating shopping list {}", id);

            ShoppingList list = shoppingListRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Shopping list not found: " + id));

            list.setName(request.name());

            // Clear existing items and rebuild from request
            list.getItems().clear();
            for (UpdateShoppingListRequest.ItemUpdate itemUpdate : request.items()) {
                ShoppingListItem item = new ShoppingListItem();
                item.setName(itemUpdate.name());
                item.setQuantity(itemUpdate.quantity());
                item.setUnit(itemUpdate.unit());
                item.setShoppingList(list);
                list.getItems().add(item);
            }

            ShoppingList saved = shoppingListRepository.save(list);
            span.setAttribute("shoppingList.itemCount", saved.getItems().size());
            log.info("Updated shopping list '{}' with {} items", saved.getName(), saved.getItems().size());

            return toDetailResponse(saved);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Transactional
    public ShoppingListDetailResponse addRecipesToShoppingList(Long id, List<Long> recipeIds) {
        Span span = tracer.spanBuilder("ShoppingListService.addRecipesToShoppingList")
                .setAttribute("shoppingList.id", id)
                .setAttribute("shoppingList.newRecipeCount", (long) recipeIds.size())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.info("Adding {} recipes to shopping list {}", recipeIds.size(), id);

            ShoppingList list = shoppingListRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Shopping list not found: " + id));

            List<Recipe> newRecipes = recipeRepository.findAllById(recipeIds);
            if (newRecipes.isEmpty()) {
                throw new IllegalArgumentException("No valid recipes found for IDs: " + recipeIds);
            }

            // Add new recipes to the list (avoid duplicates)
            List<Long> existingRecipeIds = list.getRecipes().stream()
                    .map(Recipe::getId)
                    .toList();
            List<ShoppingListItem> allItems = new ArrayList<>(list.getItems());
            for (Recipe recipe : newRecipes) {
                if (!existingRecipeIds.contains(recipe.getId())) {
                    list.getRecipes().add(recipe);
                }
                for (RecipeIngredient ri : recipe.getIngredients()) {
                    ShoppingListItem item = new ShoppingListItem();
                    item.setName(ri.getName());
                    item.setQuantity(ri.getQuantity());
                    item.setUnit(ri.getUnit());
                    allItems.add(item);
                }
            }

            // Re-combine all items (existing + new)
            list.getItems().clear();
            for (ShoppingListItem combined : combineItems(allItems)) {
                combined.setShoppingList(list);
                list.getItems().add(combined);
            }

            ShoppingList saved = shoppingListRepository.save(list);
            span.setAttribute("shoppingList.itemCount", saved.getItems().size());
            log.info("Shopping list '{}' now has {} items", saved.getName(), saved.getItems().size());

            return toDetailResponse(saved);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Transactional
    public void deleteShoppingList(Long id) {
        if (!shoppingListRepository.existsById(id)) {
            throw new IllegalArgumentException("Shopping list not found: " + id);
        }
        shoppingListRepository.deleteById(id);
        log.info("Deleted shopping list {}", id);
    }

    /**
     * Combines items with the same name (case-insensitive) by merging their
     * quantities. When units match (or one is null), quantities are summed.
     * When units differ, quantities are expressed as "qty1 unit1 + qty2 unit2".
     */
    private List<ShoppingListItem> combineItems(List<ShoppingListItem> items) {
        // Group by lowercase name only
        Map<String, ShoppingListItem> merged = new LinkedHashMap<>();

        for (ShoppingListItem item : items) {
            String key = item.getName().toLowerCase();

            if (merged.containsKey(key)) {
                ShoppingListItem existing = merged.get(key);
                mergeInto(existing, item);
            } else {
                ShoppingListItem copy = new ShoppingListItem();
                copy.setName(item.getName());
                copy.setQuantity(item.getQuantity());
                copy.setUnit(item.getUnit());
                merged.put(key, copy);
            }
        }

        return new ArrayList<>(merged.values());
    }

    private void mergeInto(ShoppingListItem existing, ShoppingListItem incoming) {
        String existingUnit = existing.getUnit() != null ? existing.getUnit().toLowerCase() : null;
        String incomingUnit = incoming.getUnit() != null ? incoming.getUnit().toLowerCase() : null;

        boolean unitsMatch = (existingUnit == null && incomingUnit == null)
                || (existingUnit != null && existingUnit.equals(incomingUnit));
        boolean oneUnitNull = (existingUnit == null) != (incomingUnit == null);

        if (unitsMatch) {
            // Same unit — sum quantities
            existing.setQuantity(sumQuantities(existing.getQuantity(), incoming.getQuantity()));
        } else if (oneUnitNull) {
            // One has a unit, the other doesn't — prefer the one with a unit
            if (existingUnit == null) {
                existing.setUnit(incoming.getUnit());
            }
            existing.setQuantity(sumQuantities(existing.getQuantity(), incoming.getQuantity()));
        } else {
            // Different non-null units — express as combined description
            String part1 = formatQtyUnit(existing.getQuantity(), existing.getUnit());
            String part2 = formatQtyUnit(incoming.getQuantity(), incoming.getUnit());
            existing.setQuantity(part1 + " + " + part2);
            existing.setUnit(null);
        }
    }

    private String sumQuantities(String q1, String q2) {
        if (q1 == null || q1.isBlank()) return q2;
        if (q2 == null || q2.isBlank()) return q1;
        try {
            double d1 = Double.parseDouble(q1);
            double d2 = Double.parseDouble(q2);
            double sum = d1 + d2;
            if (sum == Math.floor(sum) && !Double.isInfinite(sum)) {
                return String.valueOf((long) sum);
            }
            return String.valueOf(sum);
        } catch (NumberFormatException e) {
            return q1 + " + " + q2;
        }
    }

    private String formatQtyUnit(String qty, String unit) {
        if (qty != null && unit != null) return qty + " " + unit;
        if (qty != null) return qty;
        if (unit != null) return unit;
        return "";
    }

    private ShoppingListResponse toResponse(ShoppingList list) {
        return new ShoppingListResponse(
                list.getId(),
                list.getName(),
                list.getRecipes().size(),
                list.getItems().size(),
                list.getCreatedAt()
        );
    }

    private ShoppingListDetailResponse toDetailResponse(ShoppingList list) {
        List<RecipeResponse> recipes = list.getRecipes().stream()
                .map(r -> new RecipeResponse(
                        r.getId(), r.getUrl(), r.getTitle(),
                        r.getIngredients().size(), r.getCreatedAt()))
                .toList();

        List<ShoppingListItemResponse> items = list.getItems().stream()
                .map(i -> new ShoppingListItemResponse(
                        i.getId(), i.getName(), i.getQuantity(), i.getUnit()))
                .toList();

        return new ShoppingListDetailResponse(
                list.getId(), list.getName(), list.getCreatedAt(), recipes, items);
    }
}
