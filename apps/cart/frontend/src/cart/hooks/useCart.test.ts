import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchActiveCart, fetchCart, removeCartItem, updateCartItemQuantity } from '../api/cartApi';
import type { Cart } from '../model/Cart';
import { useCart } from './useCart';

vi.mock('../api/cartApi', () => ({
  fetchActiveCart: vi.fn(),
  fetchCart: vi.fn(),
  removeCartItem: vi.fn(),
  updateCartItemQuantity: vi.fn(),
}));

const cart: Cart = {
  id: 'recovered-cart', customerId: 'customer-a', status: 'ACTIVE', items: [],
  subtotal: { amount: 0, currency: 'USD' },
};

describe('useCart', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    window.history.replaceState({}, '', '/cart/');
  });

  it('recovers the active cart when no cartId appears in the URL', async () => {
    vi.mocked(fetchActiveCart).mockResolvedValue(cart);
    const { result } = renderHook(() => useCart());
    expect(result.current.isLoading).toBe(true);
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.cart).toEqual(cart);
    expect(result.current.cartId).toBe(cart.id);
    expect(fetchCart).not.toHaveBeenCalled();
  });

  it('loads an explicit cart link instead of substituting the active cart', async () => {
    window.history.replaceState({}, '', '/cart/?cartId=linked-cart');
    vi.mocked(fetchCart).mockResolvedValue({ ...cart, id: 'linked-cart' });
    const { result } = renderHook(() => useCart());
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(fetchCart).toHaveBeenCalledExactlyOnceWith('linked-cart');
    expect(fetchActiveCart).not.toHaveBeenCalled();
    expect(result.current.cartId).toBe('linked-cart');
  });

  it('finishes loading with no cart when the customer has none', async () => {
    vi.mocked(fetchActiveCart).mockResolvedValue(null);
    const { result } = renderHook(() => useCart());
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.cart).toBeNull();
    expect(result.current.cartId).toBeNull();
    expect(result.current.error).toBeNull();
  });

  it('uses the recovered cart ID for item changes', async () => {
    vi.mocked(fetchActiveCart).mockResolvedValue(cart);
    vi.mocked(updateCartItemQuantity).mockResolvedValue(cart);
    vi.mocked(removeCartItem).mockResolvedValue(cart);
    const { result } = renderHook(() => useCart());
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    await act(() => result.current.changeQuantity('item-a', 3));
    await act(() => result.current.removeItem('item-a'));

    expect(updateCartItemQuantity).toHaveBeenCalledWith(cart.id, 'item-a', 3);
    expect(removeCartItem).toHaveBeenCalledWith(cart.id, 'item-a');
  });

  it('reports lookup failures', async () => {
    vi.mocked(fetchActiveCart).mockRejectedValue(new Error('Cart service unavailable'));
    const { result } = renderHook(() => useCart());
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.error).toBe('Cart service unavailable');
  });
});
