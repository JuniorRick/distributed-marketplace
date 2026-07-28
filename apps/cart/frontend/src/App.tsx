import { useEffect, useMemo, useState } from 'react';
import { fetchCart } from './api/cartApi';
import type { Cart, Money } from './types';

const catalogFrontendUrl = import.meta.env.VITE_CATALOG_FRONTEND_URL ?? 'http://localhost:5173';

function formatMoney(money: Money) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: money.currency,
  }).format(money.amount);
}

function App() {
  const cartId = useMemo(
    () => new URLSearchParams(window.location.search).get('cartId'),
    [],
  );
  const [cart, setCart] = useState<Cart | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(cartId));
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!cartId) {
      return;
    }

    fetchCart(cartId)
      .then((response) => {
        setCart(response);
        setError(null);
      })
      .catch((requestError: Error) => setError(requestError.message))
      .finally(() => setIsLoading(false));
  }, [cartId]);

  const itemCount = cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  return (
    <main className="app-shell">
      <section className="app-header">
        <div>
          <p className="eyebrow">Distributed Marketplace</p>
          <h1>Cart</h1>
        </div>
        <a className="catalog-link" href={catalogFrontendUrl}>Continue shopping</a>
      </section>

      <section className="summary-strip" aria-label="Cart summary">
        <div>
          <span className="summary-label">Items</span>
          <strong>{itemCount}</strong>
        </div>
        <div>
          <span className="summary-label">Subtotal</span>
          <strong>{cart ? formatMoney(cart.subtotal) : '$0.00'}</strong>
        </div>
        <div>
          <span className="summary-label">Status</span>
          <strong>{cart?.status ?? 'EMPTY'}</strong>
        </div>
      </section>

      {isLoading && <p className="state-message">Loading cart...</p>}
      {error && <p className="state-message error">{error}</p>}
      {!cartId && <p className="state-message">Add a product from Catalog to start a cart.</p>}
      {cart && cart.items.length === 0 && <p className="state-message">Your cart is empty.</p>}

      {cart && cart.items.length > 0 && (
        <section className="cart-items" aria-label="Cart items">
          {cart.items.map((item) => (
            <article className="cart-item" key={item.id}>
              <div>
                <span className="cart-item__sku">{item.productSku}</span>
                <h2>{item.productName}</h2>
                <span className="cart-item__unit-price">{formatMoney(item.unitPrice)} each</span>
              </div>
              <div className="cart-item__quantity" aria-label={`Quantity ${item.quantity}`}>
                <span>Qty</span>
                <strong>{item.quantity}</strong>
              </div>
              <strong className="cart-item__total">{formatMoney(item.lineTotal)}</strong>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}

export default App;
