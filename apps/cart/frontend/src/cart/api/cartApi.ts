import type { Cart } from '../model/Cart';

const apiBaseUrl = import.meta.env.VITE_CART_API_BASE_URL ?? '';

async function cartResponse(response: Response): Promise<Cart> {
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Cart request failed with status ${response.status}`);
  }
  return response.json();
}

export async function fetchCart(cartId: string): Promise<Cart> {
  const response = await fetch(`${apiBaseUrl}/api/carts/${cartId}`);
  return cartResponse(response);
}

export async function updateCartItemQuantity(
  cartId: string,
  itemId: string,
  quantity: number,
): Promise<Cart> {
  const response = await fetch(`${apiBaseUrl}/api/carts/${cartId}/items/${itemId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity }),
  });
  return cartResponse(response);
}

export async function removeCartItem(cartId: string, itemId: string): Promise<Cart> {
  const response = await fetch(`${apiBaseUrl}/api/carts/${cartId}/items/${itemId}`, {
    method: 'DELETE',
  });
  return cartResponse(response);
}
