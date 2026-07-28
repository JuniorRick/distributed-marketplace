import { ShoppingCart } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  addProductToCart,
  cartItemCount,
  cartPageUrl,
  loadActiveCart,
  type Cart,
} from './api/cartApi';
import { fetchProducts } from './api/catalogApi';
import type { Product } from './types';

function formatPrice(product: Product) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: product.price.currency,
  }).format(product.price.amount);
}

export default function App() {
  const [products, setProducts] = useState<Product[]>([]);
  const [cart, setCart] = useState<Cart | null>(null);
  const [query, setQuery] = useState('');
  const [addingProductId, setAddingProductId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cartMessage, setCartMessage] = useState<{ text: string; error: boolean } | null>(null);

  useEffect(() => {
    fetchProducts()
      .then((data) => {
        setProducts(data);
        setError(null);
      })
      .catch((requestError: Error) => setError(requestError.message))
      .finally(() => setIsLoading(false));

    loadActiveCart()
      .then(setCart)
      .catch((requestError: Error) =>
        setCartMessage({ text: requestError.message, error: true }),
      );
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

  async function addToCart(product: Product) {
    setAddingProductId(product.id);
    setCartMessage(null);
    try {
      const updatedCart = await addProductToCart(product.id);
      setCart(updatedCart);
      setCartMessage({ text: `${product.name} added to cart.`, error: false });
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : 'Could not add product';
      setCartMessage({ text: message, error: true });
    } finally {
      setAddingProductId(null);
    }
  }

  const itemCount = cartItemCount(cart);

  return (
    <main className="app-shell">
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
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Product, SKU, description"
            />
          </label>
          <a className="cart-link" href={cartPageUrl(cart?.id)} aria-label={`Cart with ${itemCount} items`}>
            <ShoppingCart size={19} aria-hidden="true" />
            <span>Cart</span>
            <strong>{itemCount}</strong>
          </a>
        </div>
      </section>

      <section className="summary-strip" aria-label="Catalog summary">
        <div>
          <span className="summary-label">Visible products</span>
          <strong>{filteredProducts.length}</strong>
        </div>
        <div>
          <span className="summary-label">Items in cart</span>
          <strong>{itemCount}</strong>
        </div>
        <div>
          <span className="summary-label">Source</span>
          <strong>Catalog API</strong>
        </div>
      </section>

      {isLoading && <p className="state-message">Loading catalog...</p>}
      {error && <p className="state-message error">{error}</p>}
      {cartMessage && (
        <p className={`state-message cart-message${cartMessage.error ? ' error' : ' success'}`} aria-live="polite">
          {cartMessage.text}
        </p>
      )}

      {!isLoading && !error && (
        <section className="product-grid" aria-label="Products">
          {filteredProducts.map((product) => (
            <article className="product-card" key={product.id}>
              <div className="product-card__top">
                <span>{product.sku}</span>
                <span className="status-pill">{product.status}</span>
              </div>
              <h2>{product.name}</h2>
              <p>{product.description}</p>
              <div className="product-card__footer">
                <strong>{formatPrice(product)}</strong>
                <button
                  className="icon-button"
                  type="button"
                  onClick={() => addToCart(product)}
                  disabled={addingProductId === product.id}
                  aria-label={`Add ${product.name} to cart`}
                  title="Add to cart"
                >
                  <ShoppingCart size={18} aria-hidden="true" />
                </button>
              </div>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
