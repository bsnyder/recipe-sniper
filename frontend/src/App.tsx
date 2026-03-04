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

import { useState, useCallback } from 'react';
import AddRecipePage from './pages/AddRecipePage';
import ShoppingListsPage from './pages/ShoppingListsPage';
import RecipeListPage from './pages/RecipeListPage';

export default function App() {
  const [recipeRefreshKey, setRecipeRefreshKey] = useState(0);
  const [shoppingListRefreshKey, setShoppingListRefreshKey] = useState(0);

  const handleRecipeAdded = useCallback(() => {
    setRecipeRefreshKey((k) => k + 1);
  }, []);

  const handleShoppingListCreated = useCallback(() => {
    setShoppingListRefreshKey((k) => k + 1);
  }, []);

  return (
    <div className="app-container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>🎯 Recipe Sniper</h1>
        <a href="/h2-console" target="_blank" rel="noreferrer">DB Console</a>
      </div>

      <section className="app-section">
        <AddRecipePage onRecipeAdded={handleRecipeAdded} />
      </section>

      <hr className="section-divider" />

      <section className="app-section">
        <ShoppingListsPage refreshKey={shoppingListRefreshKey} />
      </section>

      <hr className="section-divider" />

      <section className="app-section">
        <RecipeListPage
          refreshKey={recipeRefreshKey}
          onShoppingListCreated={handleShoppingListCreated}
        />
      </section>
    </div>
  );
}
