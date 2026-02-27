import { useEffect, useState } from 'react';
import { getShoppingListById, deleteShoppingList, updateShoppingList } from '../api/client';
import type { ShoppingListDetailResponse, ShoppingListItemResponse } from '../types';

interface Props {
  listId: number;
  onBack: () => void;
  onDeleted: () => void;
}

interface EditableItem {
  id: number | null;
  name: string;
  quantity: string;
  unit: string;
}

function toEditable(item: ShoppingListItemResponse): EditableItem {
  return { id: item.id, name: item.name, quantity: item.quantity ?? '', unit: item.unit ?? '' };
}

export default function ShoppingListDetailPage({ listId, onBack, onDeleted }: Props) {
  const id = listId;
  const [list, setList] = useState<ShoppingListDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [checked, setChecked] = useState<Set<number>>(new Set());
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState('');
  const [editItems, setEditItems] = useState<EditableItem[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getShoppingListById(id)
      .then(setList)
      .catch((err) =>
        setError(err instanceof Error ? err.message : 'Failed to load list')
      );
  }, [id]);

  const toggleItem = (itemId: number) => {
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(itemId)) next.delete(itemId);
      else next.add(itemId);
      return next;
    });
  };

  const handleDelete = async () => {
    try {
      await deleteShoppingList(id);
      onDeleted();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
    }
  };

  const startEditing = () => {
    if (!list) return;
    setEditName(list.name);
    setEditItems(list.items.map(toEditable));
    setEditing(true);
  };

  const cancelEditing = () => {
    setEditing(false);
    setError(null);
  };

  const updateItem = (index: number, field: keyof EditableItem, value: string) => {
    setEditItems((prev) => prev.map((item, i) =>
      i === index ? { ...item, [field]: value } : item
    ));
  };

  const addItem = () => {
    setEditItems((prev) => [...prev, { id: null, name: '', quantity: '', unit: '' }]);
  };

  const removeItem = (index: number) => {
    setEditItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const updated = await updateShoppingList(id, {
        name: editName,
        items: editItems.map((item) => ({
          id: item.id,
          name: item.name,
          quantity: item.quantity || null,
          unit: item.unit || null,
        })),
      });
      setList(updated);
      setEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const formatItem = (item: { name: string; quantity: string | null; unit: string | null }) => {
    const parts: string[] = [];
    if (item.quantity) parts.push(item.quantity);
    if (item.unit) parts.push(item.unit);
    parts.push(item.name);
    return parts.join(' ');
  };

  if (error && !list) return <p style={{ color: 'red' }}>{error}</p>;
  if (!list) return <p>Loading...</p>;

  return (
    <div>
      <button onClick={onBack} style={{ marginBottom: '1rem' }}>← Back to lists</button>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
        {editing ? (
          <input
            type="text"
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            style={{ fontSize: '1.5rem', fontWeight: 'bold', padding: '0.25rem' }}
          />
        ) : (
          <h2>{list.name}</h2>
        )}
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {editing ? (
            <>
              <button onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
              <button onClick={cancelEditing}>Cancel</button>
            </>
          ) : (
            <>
              <button onClick={startEditing}>Edit</button>
              <button onClick={handleDelete} style={{ color: 'red' }}>Delete</button>
            </>
          )}
        </div>
      </div>

      <p>Created: {new Date(list.createdAt).toLocaleDateString()}</p>

      <h3>Recipes ({list.recipes.length})</h3>
      <ul>
        {list.recipes.map((r) => (
          <li key={r.id}>
            <a href={r.url} target="_blank" rel="noreferrer">{r.title}</a>
          </li>
        ))}
      </ul>

      <h3>Shopping Items ({editing ? editItems.length : list.items.length})</h3>

      {editing ? (
        <div>
          {editItems.map((item, index) => (
            <div key={index} style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
              <input
                type="text"
                value={item.quantity}
                onChange={(e) => updateItem(index, 'quantity', e.target.value)}
                placeholder="Qty"
                style={{ flex: '0 0 60px', minWidth: '50px', padding: '0.25rem' }}
              />
              <input
                type="text"
                value={item.unit}
                onChange={(e) => updateItem(index, 'unit', e.target.value)}
                placeholder="Unit"
                style={{ flex: '0 0 80px', minWidth: '60px', padding: '0.25rem' }}
              />
              <input
                type="text"
                value={item.name}
                onChange={(e) => updateItem(index, 'name', e.target.value)}
                placeholder="Item name"
                style={{ flex: '1 1 120px', minWidth: 0, padding: '0.25rem' }}
              />
              <button onClick={() => removeItem(index)} style={{ color: 'red' }}>✕</button>
            </div>
          ))}
          <button onClick={addItem} style={{ marginTop: '0.5rem' }}>+ Add Item</button>
        </div>
      ) : (
        <ul style={{ listStyleType: 'none', padding: 0 }}>
          {list.items.map((item) => (
            <li key={item.id} style={{ padding: '0.25rem 0' }}>
              <label style={{
                textDecoration: checked.has(item.id) ? 'line-through' : 'none',
                color: checked.has(item.id) ? '#999' : 'inherit',
              }}>
                <input
                  type="checkbox"
                  checked={checked.has(item.id)}
                  onChange={() => toggleItem(item.id)}
                  style={{ marginRight: '0.5rem' }}
                />
                {formatItem(item)}
              </label>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
