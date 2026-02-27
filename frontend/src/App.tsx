import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import AddRecipePage from './pages/AddRecipePage';
import RecipeListPage from './pages/RecipeListPage';
import ShoppingListsPage from './pages/ShoppingListsPage';
import ShoppingListDetailPage from './pages/ShoppingListDetailPage';

export default function App() {
  return (
    <BrowserRouter>
      <div style={{ maxWidth: '960px', margin: '0 auto', padding: '1rem' }}>
        <h1>🎯 Recipe Sniper</h1>
        <nav style={{ marginBottom: '1.5rem', display: 'flex', gap: '1rem' }}>
          <NavLink to="/">Recipes</NavLink>
          <NavLink to="/add">Add Recipe</NavLink>
          <NavLink to="/shopping-lists">Shopping Lists</NavLink>
        </nav>
        <Routes>
          <Route path="/" element={<RecipeListPage />} />
          <Route path="/add" element={<AddRecipePage />} />
          <Route path="/shopping-lists" element={<ShoppingListsPage />} />
          <Route path="/shopping-lists/:id" element={<ShoppingListDetailPage />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}
