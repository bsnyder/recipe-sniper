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

import { useEffect, useState } from 'react';
import { getAllShoppingLists, deleteShoppingList } from '../api/client';
import type { ShoppingListResponse } from '../types';
import ShoppingListDetailPage from './ShoppingListDetailPage';

interface Props {
  refreshKey: number;
}

export default function ShoppingListsPage({ refreshKey }: Props) {
  const [lists, setLists] = useState<ShoppingListResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selectedListId, setSelectedListId] = useState<number | null>(null);

  useEffect(() => {
    loadLists();
  }, [refreshKey]);

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

  if (selectedListId !== null) {
    return (
      <div>
        <h2>Shopping Lists</h2>
        <ShoppingListDetailPage
          listId={selectedListId}
          onBack={() => { setSelectedListId(null); loadLists(); }}
          onDeleted={() => { setSelectedListId(null); loadLists(); }}
        />
      </div>
    );
  }

  return (
    <div>
      <h2>Shopping Lists</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}

      {lists.length === 0 ? (
        <p>No shopping lists yet. Select recipes to create one.</p>
      ) : (
        <div className="table-wrapper">
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
                  <a
                    href="#"
                    onClick={(e) => { e.preventDefault(); setSelectedListId(l.id); }}
                  >{l.name}</a>
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
        </div>
      )}
    </div>
  );
}
