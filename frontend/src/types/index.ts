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

export interface RecipeResponse {
  id: number;
  url: string;
  title: string;
  ingredientCount: number;
  createdAt: string;
}

export interface IngredientResponse {
  id: number;
  name: string;
  quantity: string | null;
  unit: string | null;
  rawText: string;
}

export interface RecipeDetailResponse {
  id: number;
  url: string;
  title: string;
  createdAt: string;
  ingredients: IngredientResponse[];
}

export interface ShoppingListResponse {
  id: number;
  name: string;
  recipeCount: number;
  itemCount: number;
  createdAt: string;
}

export interface ShoppingListItemResponse {
  id: number;
  name: string;
  quantity: string | null;
  unit: string | null;
}

export interface ShoppingListDetailResponse {
  id: number;
  name: string;
  createdAt: string;
  recipes: RecipeResponse[];
  items: ShoppingListItemResponse[];
}

export interface UpdateShoppingListRequest {
  name: string;
  items: { id: number | null; name: string; quantity: string | null; unit: string | null }[];
}
