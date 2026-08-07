import type { Product } from '../model/Product';
import { ProductCard } from './ProductCard';

type ProductGridProps = {
  products: Product[];
  addingProductId: string | null;
  onAddToCart: (product: Product) => void;
};

export function ProductGrid({ products, addingProductId, onAddToCart }: ProductGridProps) {
  return (
    <section className="product-grid" aria-label="Products">
      {products.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          isAdding={addingProductId === product.id}
          onAddToCart={onAddToCart}
        />
      ))}
    </section>
  );
}
