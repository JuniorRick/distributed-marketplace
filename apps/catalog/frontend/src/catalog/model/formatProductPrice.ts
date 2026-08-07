import type { Product } from './Product';

export function formatProductPrice(product: Product) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: product.price.currency,
  }).format(product.price.amount);
}
