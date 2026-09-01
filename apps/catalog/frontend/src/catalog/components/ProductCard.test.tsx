import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { Product } from '../model/Product';
import { ProductCard } from './ProductCard';

const product: Product = {
  id: '0d1c3ce8-6c22-423a-94bf-ff5bf30284c2',
  sku: 'KEYBOARD-01',
  name: 'Mechanical Keyboard',
  description: 'Compact keyboard with tactile switches',
  price: { amount: 129.99, currency: 'USD' },
  status: 'ACTIVE',
  quantity: 1
};

describe('ProductCard', () => {
  it('renders product details', () => {
    render(<ProductCard product={product} isAdding={false} onAddToCart={() => undefined} />);

    expect(screen.getByRole('heading', { name: product.name })).toBeInTheDocument();
    expect(screen.getByText(product.sku)).toBeInTheDocument();
    expect(screen.getByText('$129.99')).toBeInTheDocument();
    expect(screen.getByText('qty: 1')).toBeInTheDocument();
  });

  it('adds the selected product to the cart', () => {
    const addToCart = vi.fn();
    render(<ProductCard product={product} isAdding={false} onAddToCart={addToCart} />);

    fireEvent.click(screen.getByRole('button', { name: `Add ${product.name} to cart` }));

    expect(addToCart).toHaveBeenCalledOnce();
    expect(addToCart).toHaveBeenCalledWith(product);
  });
});
