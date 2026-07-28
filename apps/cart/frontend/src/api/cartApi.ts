import type { Cart } from '../types';

const apiBaseUrl = import.meta.env.VITE_CART_API_BASE_URL ?? '';

export async function fetchCart(cartId: string): Promise<Cart> {
  const response = await fetch(`${apiBaseUrl}/api/carts/${cartId}`);
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Cart request failed with status ${response.status}`);
  }
  return response.json();
}
