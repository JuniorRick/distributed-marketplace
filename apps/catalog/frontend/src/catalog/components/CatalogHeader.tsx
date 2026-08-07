import { ShoppingCart } from 'lucide-react';

type CatalogHeaderProps = {
  query: string;
  itemCount: number;
  cartUrl: string;
  onQueryChange: (query: string) => void;
};

export function CatalogHeader({ query, itemCount, cartUrl, onQueryChange }: CatalogHeaderProps) {
  return (
    <section className="app-header">
      <div>
        <p className="eyebrow">Distributed Marketplace</p>
        <h1>Catalog</h1>
      </div>
      <div className="catalog-actions">
        <label className="search-box">
          <span>Search</span>
          <input
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Product, SKU, description"
          />
        </label>
        <a className="cart-link" href={cartUrl} aria-label={`Cart with ${itemCount} items`}>
          <ShoppingCart size={19} aria-hidden="true" />
          <span>Cart</span>
          <strong>{itemCount}</strong>
        </a>
      </div>
    </section>
  );
}
