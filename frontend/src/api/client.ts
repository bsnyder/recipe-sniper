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

import type {
  RecipeResponse,
  RecipeDetailResponse,
  ShoppingListResponse,
  ShoppingListDetailResponse,
} from '../types';

const API_BASE = '/api';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error || `Request failed: ${response.status}`);
  }
  return response.json();
}

// Recipes
export async function addRecipe(url: string): Promise<RecipeDetailResponse> {
  const res = await fetch(`${API_BASE}/recipes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ url }),
  });
  return handleResponse(res);
}

export async function getAllRecipes(search?: string): Promise<RecipeResponse[]> {
  const params = search ? `?search=${encodeURIComponent(search)}` : '';
  const res = await fetch(`${API_BASE}/recipes${params}`);
  return handleResponse(res);
}

export async function getRecipeById(id: number): Promise<RecipeDetailResponse> {
  const res = await fetch(`${API_BASE}/recipes/${id}`);
  return handleResponse(res);
}

export async function deleteRecipe(id: number): Promise<void> {
  const res = await fetch(`${API_BASE}/recipes/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete recipe');
}

// Shopping Lists
export async function createShoppingList(
  name: string,
  recipeIds: number[]
): Promise<ShoppingListDetailResponse> {
  const res = await fetch(`${API_BASE}/shopping-lists`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, recipeIds }),
  });
  return handleResponse(res);
}

export async function getAllShoppingLists(): Promise<ShoppingListResponse[]> {
  const res = await fetch(`${API_BASE}/shopping-lists`);
  return handleResponse(res);
}

export async function getShoppingListById(
  id: number
): Promise<ShoppingListDetailResponse> {
  const res = await fetch(`${API_BASE}/shopping-lists/${id}`);
  return handleResponse(res);
}

export async function updateShoppingList(
  id: number,
  data: import('../types').UpdateShoppingListRequest
): Promise<ShoppingListDetailResponse> {
  const res = await fetch(`${API_BASE}/shopping-lists/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function addRecipesToShoppingList(
  id: number,
  recipeIds: number[]
): Promise<ShoppingListDetailResponse> {
  const res = await fetch(`${API_BASE}/shopping-lists/${id}/recipes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ recipeIds }),
  });
  return handleResponse(res);
}

export async function deleteShoppingList(id: number): Promise<void> {
  const res = await fetch(`${API_BASE}/shopping-lists/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error('Failed to delete shopping list');
}
