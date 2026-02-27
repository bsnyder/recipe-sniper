import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import ShoppingListDetailPage from './ShoppingListDetailPage';
import type { ShoppingListDetailResponse } from '../types';

vi.mock('../api/client');

import { getShoppingListById } from '../api/client';

const mockGetShoppingListById = vi.mocked(getShoppingListById);

function makeList(
  overrides: Partial<ShoppingListDetailResponse> = {}
): ShoppingListDetailResponse {
  return {
    id: 1,
    name: 'Test List',
    createdAt: '2026-01-15T00:00:00Z',
    recipes: [{ id: 10, url: 'https://example.com', title: 'Soup', ingredientCount: 2, createdAt: '2026-01-15T00:00:00Z' }],
    items: [
      { id: 1, name: 'Flour', quantity: '2', unit: 'cups' },
      { id: 2, name: 'Sugar', quantity: '1', unit: 'tbsp' },
      { id: 3, name: 'Salt', quantity: null, unit: null },
    ],
    ...overrides,
  };
}

const noop = () => {};

describe('ShoppingListDetailPage handleExport', () => {
  let mockWrite: ReturnType<typeof vi.fn>;
  let mockClose: ReturnType<typeof vi.fn>;
  let mockTab: { document: { write: ReturnType<typeof vi.fn>; close: ReturnType<typeof vi.fn> } };

  beforeEach(() => {
    vi.restoreAllMocks();
    mockWrite = vi.fn();
    mockClose = vi.fn();
    mockTab = { document: { write: mockWrite, close: mockClose } };
  });

  async function renderAndWait(list: ShoppingListDetailResponse) {
    mockGetShoppingListById.mockResolvedValue(list);
    render(<ShoppingListDetailPage listId={1} onBack={noop} onDeleted={noop} />);
    await waitFor(() => expect(screen.getByText(list.name)).toBeInTheDocument());
  }

  it('calls handleExport when the Export button is clicked', async () => {
    const user = userEvent.setup();
    const list = makeList();
    await renderAndWait(list);

    vi.spyOn(window, 'open').mockReturnValue(mockTab as unknown as Window);

    await user.click(screen.getByRole('button', { name: 'Export' }));

    expect(window.open).toHaveBeenCalled();
  });

  it('formats shopping list items into a newline-separated string', async () => {
    const user = userEvent.setup();
    const list = makeList();
    await renderAndWait(list);

    vi.spyOn(window, 'open').mockReturnValue(mockTab as unknown as Window);

    await user.click(screen.getByRole('button', { name: 'Export' }));

    const written = mockWrite.mock.calls[0][0] as string;
    // Items should appear as "quantity unit name" joined by newlines inside <pre>
    expect(written).toContain('2 cups Flour\n1 tbsp Sugar\nSalt');
  });

  it('opens a new browser tab via window.open', async () => {
    const user = userEvent.setup();
    const list = makeList();
    await renderAndWait(list);

    const openSpy = vi.spyOn(window, 'open').mockReturnValue(mockTab as unknown as Window);

    await user.click(screen.getByRole('button', { name: 'Export' }));

    expect(openSpy).toHaveBeenCalledWith('', '_blank');
  });

  it('writes correct HTML content to the new tab and closes the document', async () => {
    const user = userEvent.setup();
    const list = makeList();
    await renderAndWait(list);

    vi.spyOn(window, 'open').mockReturnValue(mockTab as unknown as Window);

    await user.click(screen.getByRole('button', { name: 'Export' }));

    const written = mockWrite.mock.calls[0][0] as string;
    expect(written).toMatch(/^<html><head><title>Test List<\/title><\/head><body><pre>.*<\/pre><\/body><\/html>$/s);
    expect(mockClose).toHaveBeenCalled();
  });

  it('does nothing when the shopping list is null (still loading)', async () => {
    mockGetShoppingListById.mockReturnValue(new Promise(() => {})); // never resolves
    render(<ShoppingListDetailPage listId={1} onBack={noop} onDeleted={noop} />);
    await waitFor(() => expect(screen.getByText('Loading...')).toBeInTheDocument());

    // Export button is not rendered when list is null
    expect(screen.queryByRole('button', { name: 'Export' })).not.toBeInTheDocument();
  });

  it('exports correctly when the shopping list has no items', async () => {
    const user = userEvent.setup();
    const list = makeList({ items: [] });
    await renderAndWait(list);

    vi.spyOn(window, 'open').mockReturnValue(mockTab as unknown as Window);

    await user.click(screen.getByRole('button', { name: 'Export' }));

    const written = mockWrite.mock.calls[0][0] as string;
    expect(written).toContain('<pre></pre>');
    expect(mockClose).toHaveBeenCalled();
  });
});
