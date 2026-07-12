import { useEffect, useMemo, useState } from 'react';
import { fetchProducts } from './api/catalogApi';
import type { Product } from './types';

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

function formatPrice(product: Product) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: product.price.currency,
  }).format(product.price.amount);
}

export default function App() {
  const [products, setProducts] = useState<Product[]>([]);
  const [query, setQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchProducts()
      .then((data) => {
        setProducts(data);
        setError(null);
      })
      .catch((requestError: Error) => setError(requestError.message))
      .finally(() => setIsLoading(false));
  }, []);

  const filteredProducts = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) {
      return products;
    }

    return products.filter((product) =>
      [product.name, product.sku, product.description]
        .join(' ')
        .toLowerCase()
        .includes(normalizedQuery),
    );
  }, [products, query]);

  const visibleInventoryValue = useMemo(() => {
    return filteredProducts.reduce((total, product) => total + product.price.amount, 0);
  }, [filteredProducts]);

  return (
    <main className="catalog-shell">
      <section className="catalog-header">
        <div>
          <p className="eyebrow">Distributed Marketplace</p>
          <h1>Catalog</h1>
        </div>
        <label className="search-box">
          <span>Search</span>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Product, SKU, description"
          />
        </label>
      </section>

      <section className="summary-strip" aria-label="Catalog summary">
        <div>
          <span className="summary-label">Visible products</span>
          <strong>{filteredProducts.length}</strong>
        </div>
        <div>
          <span className="summary-label">Displayed value</span>
          <strong>{currencyFormatter.format(visibleInventoryValue)}</strong>
        </div>
        <div>
          <span className="summary-label">Source</span>
          <strong>Catalog API</strong>
        </div>
      </section>

      {isLoading && <p className="state-message">Loading catalog...</p>}
      {error && <p className="state-message error">{error}</p>}

      {!isLoading && !error && (
        <section className="product-grid" aria-label="Products">
          {filteredProducts.map((product) => (
            <article className="product-card" key={product.id}>
              <div className="product-card__top">
                <span>{product.sku}</span>
                <span className="status">{product.status}</span>
              </div>
              <h2>{product.name}</h2>
              <p>{product.description}</p>
              <strong>{formatPrice(product)}</strong>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
