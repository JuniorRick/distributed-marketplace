import type { Order } from '../model/Order';
import { authenticatedFetch } from '@marketplace/auth';

const ordersApiBaseUrl = import.meta.env.VITE_ORDERS_API_BASE_URL ?? '';
const marketplaceUrl = import.meta.env.VITE_MARKETPLACE_URL ?? 'http://localhost:8080';
const ordersFrontendUrl = import.meta.env.VITE_ORDERS_FRONTEND_URL ?? `${marketplaceUrl}/orders/`;

async function orderResponse(response: Response): Promise<Order> {
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Order request failed with status ${response.status}`);
  }
  return response.json();
}

export async function createOrderFromCart(cartId: string): Promise<Order> {
  const response = await authenticatedFetch(`${ordersApiBaseUrl}/api/orders`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cartId }),
  });
  return orderResponse(response);
}

export function orderPageUrl(orderId: string) {
  const url = new URL(ordersFrontendUrl, window.location.origin);
  url.searchParams.set('orderId', orderId);
  return url.toString();
}
