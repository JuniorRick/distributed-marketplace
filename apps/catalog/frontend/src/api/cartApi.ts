export type Money = {
  amount: number;
  currency: string;
};

export type CartItem = {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  unitPrice: Money;
  quantity: number;
  lineTotal: Money;
};

export type Cart = {
  id: string;
  customerId: string;
  status: 'ACTIVE' | 'CHECKED_OUT' | 'ABANDONED';
  items: CartItem[];
  subtotal: Money;
};

const cartApiBaseUrl = import.meta.env.VITE_CART_API_BASE_URL ?? '/cart-api';
const cartFrontendUrl = import.meta.env.VITE_CART_FRONTEND_URL ?? 'http://localhost:5174';
const cartIdStorageKey = 'marketplace.activeCartId';
const customerIdStorageKey = 'marketplace.guestCustomerId';

async function responseError(response: Response) {
  const body = await response.json().catch(() => null);
  return new Error(body?.message ?? `Cart request failed with status ${response.status}`);
}

function guestCustomerId() {
  const stored = localStorage.getItem(customerIdStorageKey);
  if (stored) {
    return stored;
  }

  const created = crypto.randomUUID();
  localStorage.setItem(customerIdStorageKey, created);
  return created;
}

async function createCart(): Promise<Cart> {
  const response = await fetch(`${cartApiBaseUrl}/api/carts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ customerId: guestCustomerId() }),
  });

  if (!response.ok) {
    throw await responseError(response);
  }

  const cart: Cart = await response.json();
  localStorage.setItem(cartIdStorageKey, cart.id);
  return cart;
}

async function postItem(cartId: string, productId: string): Promise<Response> {
  return fetch(`${cartApiBaseUrl}/api/carts/${cartId}/items`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ productId, quantity: 1 }),
  });
}

export async function loadActiveCart(): Promise<Cart | null> {
  const cartId = localStorage.getItem(cartIdStorageKey);
  if (!cartId) {
    return null;
  }

  const response = await fetch(`${cartApiBaseUrl}/api/carts/${cartId}`);
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

export function cartItemCount(cart: Cart | null) {
  return cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;
}
