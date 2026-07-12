import type { Product } from '../types';

const apiBaseUrl = import.meta.env.VITE_CATALOG_API_BASE_URL ?? '';

export async function fetchProducts(): Promise<Product[]> {
  const response = await fetch(`${apiBaseUrl}/api/products`);

  if (!response.ok) {
    throw new Error(`Catalog request failed with status ${response.status}`);
  }

  return response.json();
}
