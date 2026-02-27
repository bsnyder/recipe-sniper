package com.recipesniper.controller;

import com.recipesniper.dto.AddRecipesToShoppingListRequest;
import com.recipesniper.dto.CreateShoppingListRequest;
import com.recipesniper.dto.ShoppingListDetailResponse;
import com.recipesniper.dto.ShoppingListResponse;
import com.recipesniper.dto.UpdateShoppingListRequest;
import com.recipesniper.service.ShoppingListService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @PostMapping
    public ResponseEntity<ShoppingListDetailResponse> createShoppingList(
            @Valid @RequestBody CreateShoppingListRequest request) {
        ShoppingListDetailResponse response =
                shoppingListService.createShoppingList(request.name(), request.recipeIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ShoppingListResponse>> getAllShoppingLists() {
        return ResponseEntity.ok(shoppingListService.getAllShoppingLists());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListDetailResponse> getShoppingListById(@PathVariable Long id) {
        return ResponseEntity.ok(shoppingListService.getShoppingListById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListDetailResponse> updateShoppingList(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShoppingListRequest request) {
        return ResponseEntity.ok(shoppingListService.updateShoppingList(id, request));
    }

    @PostMapping("/{id}/recipes")
    public ResponseEntity<ShoppingListDetailResponse> addRecipesToShoppingList(
            @PathVariable Long id,
            @Valid @RequestBody AddRecipesToShoppingListRequest request) {
        return ResponseEntity.ok(
                shoppingListService.addRecipesToShoppingList(id, request.recipeIds()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShoppingList(@PathVariable Long id) {
        shoppingListService.deleteShoppingList(id);
        return ResponseEntity.noContent().build();
    }
}
