/*
 * Copyright 2026 Bruce Snyder (bsnyder@apache.org)
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.recipesniper.repository;

import com.recipesniper.entity.Recipe;
import com.recipesniper.entity.ShoppingList;
import com.recipesniper.entity.ShoppingListItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ShoppingListRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Test
    void shouldSaveAndFindShoppingList() {
        ShoppingList list = new ShoppingList();
        list.setName("Weekly Groceries");

        ShoppingList saved = shoppingListRepository.save(list);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<ShoppingList> found = shoppingListRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Weekly Groceries");
    }

    @Test
    void shouldSaveShoppingListWithItems() {
        ShoppingList list = new ShoppingList();
        list.setName("Weekly Groceries");

        ShoppingListItem item = new ShoppingListItem();
        item.setName("flour");
        item.setQuantity("2");
        item.setUnit("cups");
        item.setShoppingList(list);
        list.getItems().add(item);

        ShoppingList saved = shoppingListRepository.save(list);
        entityManager.flush();
        entityManager.clear();

        ShoppingList found = shoppingListRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getName()).isEqualTo("flour");
    }

    @Test
    void shouldSaveShoppingListWithRecipes() {
        Recipe recipe = new Recipe();
        recipe.setUrl("https://example.com/recipe");
        recipe.setTitle("Test Recipe");
        recipe.setRawHtml("<html>test</html>");
        entityManager.persistAndFlush(recipe);

        ShoppingList list = new ShoppingList();
        list.setName("Weekly Groceries");
        list.getRecipes().add(recipe);

        ShoppingList saved = shoppingListRepository.save(list);
        entityManager.flush();
        entityManager.clear();

        ShoppingList found = shoppingListRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRecipes()).hasSize(1);
        assertThat(found.getRecipes().get(0).getTitle()).isEqualTo("Test Recipe");
    }

    @Test
    void shouldCascadeDeleteItems() {
        ShoppingList list = new ShoppingList();
        list.setName("Weekly Groceries");

        ShoppingListItem item = new ShoppingListItem();
        item.setName("butter");
        item.setQuantity("1");
        item.setUnit("stick");
        item.setShoppingList(list);
        list.getItems().add(item);

        ShoppingList saved = shoppingListRepository.save(list);
        entityManager.flush();

        shoppingListRepository.deleteById(saved.getId());
        entityManager.flush();

        assertThat(shoppingListRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void shouldDeleteShoppingList() {
        ShoppingList list = new ShoppingList();
        list.setName("Weekly Groceries");

        ShoppingList saved = shoppingListRepository.save(list);
        shoppingListRepository.deleteById(saved.getId());

        assertThat(shoppingListRepository.findById(saved.getId())).isEmpty();
    }
}
