import type { Cart } from '../model/Cart';
import { authenticatedFetch, currentUser } from '@marketplace/auth';

const cartApiBaseUrl = import.meta.env.VITE_CART_API_BASE_URL ?? '';
const marketplaceUrl = import.meta.env.VITE_MARKETPLACE_URL ?? 'http://localhost:8080';
const cartFrontendUrl = import.meta.env.VITE_CART_FRONTEND_URL ?? `${marketplaceUrl}/cart/`;
const cartIdStorageKey = 'marketplace.activeCartId';

async function responseError(response: Response) {
  const body = await response.json().catch(() => null);
  return new Error(body?.message ?? `Cart request failed with status ${response.status}`);
}

async function createCart(): Promise<Cart> {
  const response = await authenticatedFetch(`${cartApiBaseUrl}/api/carts`, {
    method: 'POST',
  });

  if (!response.ok) {
    throw await responseError(response);
  }

  const cart: Cart = await response.json();
  localStorage.setItem(cartIdStorageKey, cart.id);
  return cart;
}

async function postItem(cartId: string, productId: string): Promise<Response> {
  return authenticatedFetch(`${cartApiBaseUrl}/api/carts/${cartId}/items`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ productId, quantity: 1 }),
  });
}

export async function loadActiveCart(): Promise<Cart | null> {
  if (!currentUser()) {
    return null;
  }
  const cartId = localStorage.getItem(cartIdStorageKey);
  if (!cartId) {
    return null;
  }

  const response = await authenticatedFetch(`${cartApiBaseUrl}/api/carts/${cartId}`);
  if (response.status === 404) {
    localStorage.removeItem(cartIdStorageKey);
    return null;
  }
  if (!response.ok) {
    throw await responseError(response);
  }

  const cart: Cart = await response.json();
  if (cart.status !== 'ACTIVE') {
    localStorage.removeItem(cartIdStorageKey);
    return null;
  }
  return cart;
}

export async function addProductToCart(productId: string): Promise<Cart> {
  let cartId = localStorage.getItem(cartIdStorageKey);
  if (!cartId) {
    cartId = (await createCart()).id;
  }

  let response = await postItem(cartId, productId);
  if (response.status === 404 || response.status === 409) {
    localStorage.removeItem(cartIdStorageKey);
    cartId = (await createCart()).id;
    response = await postItem(cartId, productId);
  }
  if (!response.ok) {
    throw await responseError(response);
  }

  return response.json();
}

export function cartPageUrl(cartId?: string) {
  const url = new URL(cartFrontendUrl, window.location.origin);
  if (cartId) {
    url.searchParams.set('cartId', cartId);
  }
  return url.toString();
}
