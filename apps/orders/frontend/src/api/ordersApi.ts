import type { Order } from '../types';

const apiBaseUrl = import.meta.env.VITE_ORDERS_API_BASE_URL ?? '';

export async function fetchOrder(orderId: string): Promise<Order> {
  const response = await fetch(`${apiBaseUrl}/api/orders/${orderId}`);
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Order request failed with status ${response.status}`);
  }
  return response.json();
}
