import type { Order } from '../model/Order';

const ordersApiBaseUrl = import.meta.env.VITE_ORDERS_API_BASE_URL ?? '/orders-api';
const ordersFrontendUrl = import.meta.env.VITE_ORDERS_FRONTEND_URL ?? 'http://localhost:5175';

async function orderResponse(response: Response): Promise<Order> {
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Order request failed with status ${response.status}`);
  }
  return response.json();
}

export async function createOrderFromCart(cartId: string): Promise<Order> {
  const response = await fetch(`${ordersApiBaseUrl}/api/orders`, {
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
