import { useEffect, useState } from 'react';
import { getAllShoppingLists, deleteShoppingList } from '../api/client';
import type { ShoppingListResponse } from '../types';
import { useNavigate } from 'react-router-dom';

export default function ShoppingListsPage() {
  const [lists, setLists] = useState<ShoppingListResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadLists();
  }, []);

  const loadLists = async () => {
    try {
      setLists(await getAllShoppingLists());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load lists');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteShoppingList(id);
      setLists((prev) => prev.filter((l) => l.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
    }
  };

  return (
    <div>
      <h2>Shopping Lists</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {lists.length === 0 ? (
        <p>No shopping lists yet. Select recipes to create one.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left', padding: '0.5rem' }}>Name</th>
              <th style={{ textAlign: 'left', padding: '0.5rem' }}>Recipes</th>
              <th style={{ textAlign: 'left', padding: '0.5rem' }}>Items</th>
              <th style={{ textAlign: 'left', padding: '0.5rem' }}>Created</th>
              <th style={{ textAlign: 'left', padding: '0.5rem' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {lists.map((l) => (
              <tr key={l.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '0.5rem' }}>
                  <a href={`/shopping-lists/${l.id}`} onClick={(e) => {
                    e.preventDefault();
                    navigate(`/shopping-lists/${l.id}`);
                  }}>{l.name}</a>
                </td>
                <td style={{ padding: '0.5rem' }}>{l.recipeCount}</td>
                <td style={{ padding: '0.5rem' }}>{l.itemCount}</td>
                <td style={{ padding: '0.5rem' }}>
                  {new Date(l.createdAt).toLocaleDateString()}
                </td>
                <td style={{ padding: '0.5rem' }}>
                  <button onClick={() => handleDelete(l.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
