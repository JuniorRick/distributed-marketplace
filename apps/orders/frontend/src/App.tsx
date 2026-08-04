import { useEffect, useMemo, useState } from 'react';
import { fetchOrder } from './api/ordersApi';
import type { Money, Order } from './types';

const catalogFrontendUrl = import.meta.env.VITE_CATALOG_FRONTEND_URL ?? 'http://localhost:5173';

function formatMoney(money: Money) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: money.currency,
  }).format(money.amount);
}

function App() {
  const orderId = useMemo(
    () => new URLSearchParams(window.location.search).get('orderId'),
    [],
  );
  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(orderId));
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderId) {
      return;
    }

    let cancelled = false;
    let pollTimer: number | undefined;

    async function loadOrder() {
      try {
        const response = await fetchOrder(orderId!);
        if (cancelled) {
          return;
        }
        setOrder(response);
        setError(null);
        setIsLoading(false);
        if (response.status === 'PENDING') {
          pollTimer = window.setTimeout(loadOrder, 1000);
        }
      } catch (requestError) {
        if (!cancelled) {
          const message = requestError instanceof Error ? requestError.message : 'Could not load order';
          setError(message);
          setIsLoading(false);
        }
      }
    }

    loadOrder();
    return () => {
      cancelled = true;
      window.clearTimeout(pollTimer);
    };
  }, [orderId]);

  const itemCount = order?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  return (
    <main className="app-shell">
      <section className="app-header">
        <div>
          <p className="eyebrow">Distributed Marketplace</p>
          <h1>Order</h1>
        </div>
        <a className="catalog-link" href={catalogFrontendUrl}>Continue shopping</a>
      </section>

      <section className="summary-strip" aria-label="Order summary">
        <div>
          <span className="summary-label">Items</span>
          <strong>{itemCount}</strong>
        </div>
        <div>
          <span className="summary-label">Total</span>
          <strong>{order ? formatMoney(order.total) : '$0.00'}</strong>
        </div>
        <div>
          <span className="summary-label">Status</span>
          <strong>{order?.status ?? 'UNKNOWN'}</strong>
        </div>
      </section>

      {isLoading && <p className="state-message">Loading order...</p>}
      {error && <p className="state-message error">{error}</p>}
      {!orderId && <p className="state-message">Create an order from Cart to view it here.</p>}
      {order?.status === 'PENDING' && (
        <p className="state-message processing">Checkout is being processed...</p>
      )}
      {order?.status === 'REJECTED' && (
        <p className="state-message error">
          Checkout failed: {order.failureReason ?? 'Cart rejected the checkout request'}
        </p>
      )}

      {order && (
        <>
          <section className="order-reference" aria-label="Order reference">
            <span>Order</span>
            <strong>{order.id}</strong>
            <span>{new Date(order.createdAt).toLocaleString()}</span>
          </section>

          <section className="order-items" aria-label="Order items">
            {order.items.map((item) => (
              <article className="order-item" key={item.id}>
                <div>
                  <span className="order-item__sku">{item.productSku}</span>
                  <h2>{item.productName}</h2>
                  <span className="order-item__unit-price">{formatMoney(item.unitPrice)} each</span>
                </div>
                <div className="order-item__quantity">
                  <span>Qty</span>
                  <strong>{item.quantity}</strong>
                </div>
                <strong className="order-item__total">{formatMoney(item.lineTotal)}</strong>
              </article>
            ))}
          </section>
        </>
      )}
    </main>
  );
}

export default App;
