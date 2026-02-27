import { useEffect, useState } from 'react';
import { getAllRecipes, deleteRecipe, createShoppingList, getAllShoppingLists, addRecipesToShoppingList } from '../api/client';
import type { RecipeResponse, ShoppingListResponse } from '../types';

interface Props {
  refreshKey: number;
  onShoppingListCreated?: () => void;
}

export default function RecipeListPage({ refreshKey, onShoppingListCreated }: Props) {
  const [recipes, setRecipes] = useState<RecipeResponse[]>([]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [listName, setListName] = useState('');
  const [search, setSearch] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [existingLists, setExistingLists] = useState<ShoppingListResponse[]>([]);
  const [selectedListId, setSelectedListId] = useState<number | ''>('');

  useEffect(() => {
    loadRecipes();
    getAllShoppingLists().then(setExistingLists).catch(() => {});
  }, [refreshKey]);

  const loadRecipes = async (query?: string) => {
    try {
      setRecipes(await getAllRecipes(query));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load recipes');
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadRecipes(search || undefined);
  };

  const toggleSelect = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteRecipe(id);
      setRecipes((prev) => prev.filter((r) => r.id !== id));
      setSelected((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
    }
  };

  const handleCreateList = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selected.size === 0) return;
    try {
      await createShoppingList(
        listName || 'Shopping List',
        Array.from(selected)
      );
      setSelected(new Set());
      setListName('');
      onShoppingListCreated?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create list');
    }
  };

  return (
    <div>
      <h2>Recipes</h2>

      <form onSubmit={handleSearch} style={{ marginBottom: '1rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search recipes by title..."
          style={{ flex: 1, padding: '0.5rem' }}
        />
        <button type="submit">Search</button>
        {search && (
          <button type="button" onClick={() => { setSearch(''); loadRecipes(); }}>
            Clear
          </button>
        )}
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {recipes.length === 0 ? (
        <p>{search ? 'No recipes match your search.' : 'No recipes yet. Add one from the Add Recipe page.'}</p>
      ) : (
        <>
          <div className="table-wrapper">
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th style={{ textAlign: 'left', padding: '0.5rem' }}>Select</th>
                <th style={{ textAlign: 'left', padding: '0.5rem' }}>Title</th>
                <th style={{ textAlign: 'left', padding: '0.5rem' }}>Ingredients</th>
                <th style={{ textAlign: 'left', padding: '0.5rem' }}>Added</th>
                <th style={{ textAlign: 'left', padding: '0.5rem' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {recipes.map((r) => (
                <tr key={r.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '0.5rem' }}>
                    <input
                      type="checkbox"
                      checked={selected.has(r.id)}
                      onChange={() => toggleSelect(r.id)}
                    />
                  </td>
                  <td style={{ padding: '0.5rem' }}>
                    {r.title}
                  </td>
                  <td style={{ padding: '0.5rem' }}>{r.ingredientCount}</td>
                  <td style={{ padding: '0.5rem' }}>
                    {new Date(r.createdAt).toLocaleDateString()}
                  </td>
                  <td style={{ padding: '0.5rem' }}>
                    <button onClick={() => handleDelete(r.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>

          {selected.size > 0 && (
            <div style={{ marginTop: '1rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <form onSubmit={handleCreateList} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                <input
                  type="text"
                  value={listName}
                  onChange={(e) => setListName(e.target.value)}
                  placeholder="Shopping list name..."
                  style={{ padding: '0.5rem' }}
                />
                <button type="submit">
                  Create New List ({selected.size} recipe{selected.size > 1 ? 's' : ''})
                </button>
              </form>

              {existingLists.length > 0 && (
                <form
                  onSubmit={async (e) => {
                    e.preventDefault();
                    if (!selectedListId) return;
                    try {
                      await addRecipesToShoppingList(
                        selectedListId,
                        Array.from(selected)
                      );
                      setSelected(new Set());
                      onShoppingListCreated?.();
                    } catch (err) {
                      setError(err instanceof Error ? err.message : 'Failed to add recipes');
                    }
                  }}
                  style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}
                >
                  <span>or add to:</span>
                  <select
                    value={selectedListId}
                    onChange={(e) => setSelectedListId(e.target.value ? Number(e.target.value) : '')}
                    style={{ padding: '0.5rem' }}
                  >
                    <option value="">Select existing list...</option>
                    {existingLists.map((l) => (
                      <option key={l.id} value={l.id}>{l.name}</option>
                    ))}
                  </select>
                  <button type="submit" disabled={!selectedListId}>
                    Add to List
                  </button>
                </form>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
