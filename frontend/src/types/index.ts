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
