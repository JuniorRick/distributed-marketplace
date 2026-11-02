import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authenticatedFetch, currentUser } from '@marketplace/auth';
import { addProductToCart, loadActiveCart } from './cartApi';

vi.mock('@marketplace/auth', () => ({
  authenticatedFetch: vi.fn(),
  currentUser: vi.fn(),
}));

const fetchMock = vi.mocked(authenticatedFetch);
const userMock = vi.mocked(currentUser);

describe('customer cart recovery', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
    userMock.mockReturnValue({ subject: 'customer-a', email: 'a@example.com', displayName: 'A' });
  });

  it('does not request a cart for anonymous visitors', async () => {
    userMock.mockReturnValue(null);
    expect(await loadActiveCart()).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('recovers the authenticated customer cart without relying on a cached cart ID', async () => {
    localStorage.setItem('marketplace.activeCartId', 'another-customer-cart');
    const cart = { id: 'own-cart', status: 'ACTIVE', items: [] };
    fetchMock.mockResolvedValue(Response.json(cart));

    expect(await loadActiveCart()).toEqual(cart);
    expect(fetchMock).toHaveBeenCalledExactlyOnceWith('/api/carts/active');
  });

  it('treats an absent active cart as empty', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    expect(await loadActiveCart()).toBeNull();
  });

  it('keeps a failed lookup visible instead of treating it as an empty cart', async () => {
    fetchMock.mockResolvedValue(Response.json({ message: 'Cart service unavailable' }, { status: 503 }));
    await expect(loadActiveCart()).rejects.toThrow('Cart service unavailable');
  });

  it('resolves a fresh cart for each account before adding a product', async () => {
    localStorage.setItem('marketplace.activeCartId', 'legacy-cart');
    fetchMock.mockResolvedValueOnce(Response.json({ id: 'cart-a' }))
      .mockResolvedValueOnce(Response.json({ id: 'cart-a' }))
      .mockResolvedValueOnce(Response.json({ id: 'cart-b' }))
      .mockResolvedValueOnce(Response.json({ id: 'cart-b' }));

    await addProductToCart('product-a');
    userMock.mockReturnValue({ subject: 'customer-b', email: 'b@example.com', displayName: 'B' });
    await addProductToCart('product-b');

    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/carts', '/api/carts/cart-a/items', '/api/carts', '/api/carts/cart-b/items',
    ]);
    expect(JSON.parse(fetchMock.mock.calls[3][1]!.body as string)).toEqual({
      productId: 'product-b', quantity: 1,
    });
  });
});
