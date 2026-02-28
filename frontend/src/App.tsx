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
