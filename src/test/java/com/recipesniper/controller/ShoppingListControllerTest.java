package com.recipesniper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipesniper.dto.AddRecipesToShoppingListRequest;
import com.recipesniper.dto.CreateShoppingListRequest;
import com.recipesniper.dto.ShoppingListDetailResponse;
import com.recipesniper.dto.ShoppingListItemResponse;
import com.recipesniper.dto.ShoppingListResponse;
import com.recipesniper.dto.UpdateShoppingListRequest;
import com.recipesniper.service.ShoppingListService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShoppingListController.class)
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ShoppingListService shoppingListService;

    @Test
    void shouldCreateShoppingList() throws Exception {
        var request = new CreateShoppingListRequest("Weekly", List.of(1L, 2L));
        var response = new ShoppingListDetailResponse(
                1L, "Weekly", LocalDateTime.now(), List.of(),
                List.of(new ShoppingListItemResponse(1L, "flour", "2", "cups")));

        when(shoppingListService.createShoppingList("Weekly", List.of(1L, 2L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/shopping-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Weekly"))
                .andExpect(jsonPath("$.items[0].name").value("flour"));
    }

    @Test
    void shouldReturnBadRequestForEmptyRecipeIds() throws Exception {
        var request = new CreateShoppingListRequest("Weekly", List.of());

        mockMvc.perform(post("/api/shopping-lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetAllShoppingLists() throws Exception {
        var lists = List.of(
                new ShoppingListResponse(1L, "Weekly", 2, 5, LocalDateTime.now()));
        when(shoppingListService.getAllShoppingLists()).thenReturn(lists);

        mockMvc.perform(get("/api/shopping-lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Weekly"));
    }

    @Test
    void shouldGetShoppingListById() throws Exception {
        var response = new ShoppingListDetailResponse(
                1L, "Weekly", LocalDateTime.now(), List.of(),
                List.of(new ShoppingListItemResponse(1L, "flour", "2", "cups")));
        when(shoppingListService.getShoppingListById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/shopping-lists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Weekly"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void shouldReturn404WhenShoppingListNotFound() throws Exception {
        when(shoppingListService.getShoppingListById(99L))
                .thenThrow(new IllegalArgumentException("Shopping list not found: 99"));

        mockMvc.perform(get("/api/shopping-lists/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateShoppingList() throws Exception {
        var request = new UpdateShoppingListRequest("Updated", List.of(
                new UpdateShoppingListRequest.ItemUpdate(null, "butter", "1", "stick")));
        var response = new ShoppingListDetailResponse(
                1L, "Updated", LocalDateTime.now(), List.of(),
                List.of(new ShoppingListItemResponse(2L, "butter", "1", "stick")));

        when(shoppingListService.updateShoppingList(eq(1L), any(UpdateShoppingListRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/shopping-lists/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.items[0].name").value("butter"));
    }

    @Test
    void shouldAddRecipesToShoppingList() throws Exception {
        var request = new AddRecipesToShoppingListRequest(List.of(2L));
        var response = new ShoppingListDetailResponse(
                1L, "Weekly", LocalDateTime.now(), List.of(),
                List.of(
                        new ShoppingListItemResponse(1L, "flour", "2", "cups"),
                        new ShoppingListItemResponse(2L, "yeast", "1", "packet")));

        when(shoppingListService.addRecipesToShoppingList(eq(1L), eq(List.of(2L))))
                .thenReturn(response);

        mockMvc.perform(post("/api/shopping-lists/1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void shouldDeleteShoppingList() throws Exception {
        mockMvc.perform(delete("/api/shopping-lists/1"))
                .andExpect(status().isNoContent());

        verify(shoppingListService).deleteShoppingList(1L);
    }
}
