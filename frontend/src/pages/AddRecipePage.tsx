import { useState } from 'react';
import { addRecipe } from '../api/client';
import type { RecipeDetailResponse } from '../types';

interface Props {
  onRecipeAdded?: () => void;
}

export default function AddRecipePage({ onRecipeAdded }: Props) {
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<RecipeDetailResponse | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const recipe = await addRecipe(url);
      setResult(recipe);
      setUrl('');
      onRecipeAdded?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add recipe');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>Add Recipe</h2>
      <form onSubmit={handleSubmit} style={{ marginBottom: '1rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        <input
          type="url"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="Paste recipe URL..."
          required
          style={{ flex: '1 1 200px', minWidth: 0, padding: '0.5rem' }}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Scraping...' : 'Add Recipe'}
        </button>
      </form>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      {result && (
        <div style={{ border: '1px solid #ccc', padding: '1rem', borderRadius: '8px' }}>
          <h3>{result.title}</h3>
          <p>
            <a href={result.url} target="_blank" rel="noreferrer">{result.url}</a>
          </p>
          <h4>Ingredients ({result.ingredients.length})</h4>
          <ul>
            {result.ingredients.map((ing) => (
              <li key={ing.id}>{ing.rawText}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
