import { ShoppingCart } from 'lucide-react';
import { formatProductPrice } from '../model/formatProductPrice';
import type { Product } from '../model/Product';

type ProductCardProps = {
  product: Product;
  isAdding: boolean;
  onAddToCart: (product: Product) => void;
};

export function ProductCard({ product, isAdding, onAddToCart }: ProductCardProps) {
  return (
    <article className="product-card">
      <div className="product-card__top">
        <span>{product.sku}</span>
        <span className="status-pill">{product.status}</span>
      </div>
      <h2>{product.name}</h2>
      <p>{product.description}</p>
      <div className="product-card__footer">
        <strong>{formatProductPrice(product)}</strong>
        <button
          className="icon-button"
          type="button"
          onClick={() => onAddToCart(product)}
          disabled={isAdding}
          aria-label={`Add ${product.name} to cart`}
          title="Add to cart"
        >
          <ShoppingCart size={18} aria-hidden="true" />
        </button>
      </div>
    </article>
  );
}
