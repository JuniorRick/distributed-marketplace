import { useEffect, useMemo, useState } from 'react';
import { fetchCart, removeCartItem, updateCartItemQuantity } from './api/cartApi';
import { createOrderFromCart, orderPageUrl } from './api/orderApi';
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
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [changingItemId, setChangingItemId] = useState<string | null>(null);

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
  const canCheckout = cart?.status === 'ACTIVE' && itemCount > 0;

  async function changeQuantity(itemId: string, quantity: number) {
    if (!cartId) {
      return;
    }

    setChangingItemId(itemId);
    setError(null);
    try {
      setCart(await updateCartItemQuantity(cartId, itemId, quantity));
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : 'Could not change quantity';
      setError(message);
    } finally {
      setChangingItemId(null);
    }
  }

  async function removeItem(itemId: string) {
    if (!cartId) {
      return;
    }

    setChangingItemId(itemId);
    setError(null);
    try {
      setCart(await removeCartItem(cartId, itemId));
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : 'Could not remove item';
      setError(message);
    } finally {
      setChangingItemId(null);
    }
  }

  async function checkout() {
    if (!cartId || !canCheckout) {
      return;
    }

    setIsCheckingOut(true);
    setError(null);
    try {
      const order = await createOrderFromCart(cartId);
      window.location.assign(orderPageUrl(order.id));
    } catch (requestError) {
      const message = requestError instanceof Error ? requestError.message : 'Could not create order';
      setError(message);
      setIsCheckingOut(false);
    }
  }

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
        <>
          <section className="cart-items" aria-label="Cart items">
            {cart.items.map((item) => (
              <article className="cart-item" key={item.id}>
                <div>
                  <span className="cart-item__sku">{item.productSku}</span>
                  <h2>{item.productName}</h2>
                  <span className="cart-item__unit-price">{formatMoney(item.unitPrice)} each</span>
                </div>
                <div className="cart-item__actions">
                  <div className="quantity-stepper" aria-label={`Quantity for ${item.productName}`}>
                    <button
                      type="button"
                      onClick={() => changeQuantity(item.id, item.quantity - 1)}
                      disabled={
                        item.quantity === 1 ||
                        changingItemId === item.id ||
                        isCheckingOut ||
                        cart.status !== 'ACTIVE'
                      }
                      aria-label={`Decrease ${item.productName} quantity`}
                    >
                      -
                    </button>
                    <output aria-live="polite">{item.quantity}</output>
                    <button
                      type="button"
                      onClick={() => changeQuantity(item.id, item.quantity + 1)}
                      disabled={
                        item.quantity === 99 ||
                        changingItemId === item.id ||
                        isCheckingOut ||
                        cart.status !== 'ACTIVE'
                      }
                      aria-label={`Increase ${item.productName} quantity`}
                    >
                      +
                    </button>
                  </div>
                  <button
                    className="remove-item-button"
                    type="button"
                    onClick={() => removeItem(item.id)}
                    disabled={
                      changingItemId === item.id ||
                      isCheckingOut ||
                      cart.status !== 'ACTIVE'
                    }
                  >
                    Remove
                  </button>
                </div>
                <strong className="cart-item__total">{formatMoney(item.lineTotal)}</strong>
              </article>
            ))}
          </section>

          <section className="checkout-bar" aria-label="Checkout">
            <div>
              <span className="summary-label">Order total</span>
              <strong>{formatMoney(cart.subtotal)}</strong>
            </div>
            <button
              type="button"
              className="checkout-button"
              onClick={checkout}
              disabled={!canCheckout || isCheckingOut || changingItemId !== null}
            >
              {isCheckingOut ? 'Creating order...' : cart.status === 'CHECKED_OUT' ? 'Checked out' : 'Checkout'}
            </button>
          </section>
        </>
      )}
    </main>
  );
}

export default App;
