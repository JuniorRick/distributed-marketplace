import { useCallback } from 'react';
import { useCheckout } from '../../order/hooks/useCheckout';
import { CartHeader } from '../components/CartHeader';
import { CartItemList } from '../components/CartItemList';
import { CartSummary } from '../components/CartSummary';
import { CheckoutBar } from '../components/CheckoutBar';
import { useCart } from '../hooks/useCart';

export function CartPage() {
  const {
    cartId,
    cart,
    itemCount,
    isLoading,
    error,
    setError,
    changingItemId,
    changeQuantity,
    removeItem,
  } = useCart();
  const canCheckout = cart?.status === 'ACTIVE' && itemCount > 0;
  const handleCheckoutError = useCallback((message: string | null) => setError(message), [setError]);
  const { isCheckingOut, checkout } = useCheckout(cartId, canCheckout, handleCheckoutError);

  return (
    <main className="app-shell">
      <CartHeader />
      <CartSummary cart={cart} itemCount={itemCount} />

      {isLoading && <p className="state-message">Loading cart...</p>}
      {error && <p className="state-message error">{error}</p>}
      {!cartId && <p className="state-message">Add a product from Catalog to start a cart.</p>}
      {cart && cart.items.length === 0 && <p className="state-message">Your cart is empty.</p>}

      {cart && cart.items.length > 0 && (
        <>
          <CartItemList
            cart={cart}
            changingItemId={changingItemId}
            isCheckingOut={isCheckingOut}
            onQuantityChange={changeQuantity}
            onRemove={removeItem}
          />
          <CheckoutBar
            cart={cart}
            canCheckout={canCheckout}
            isCheckingOut={isCheckingOut}
            isChangingItem={changingItemId !== null}
            onCheckout={checkout}
          />
        </>
      )}
    </main>
  );
}
