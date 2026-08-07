import { formatMoney } from '../../shared/formatMoney';
import type { Cart } from '../model/Cart';

type CheckoutBarProps = {
  cart: Cart;
  canCheckout: boolean;
  isCheckingOut: boolean;
  isChangingItem: boolean;
  onCheckout: () => void;
};

export function CheckoutBar({
  cart,
  canCheckout,
  isCheckingOut,
  isChangingItem,
  onCheckout,
}: CheckoutBarProps) {
  return (
    <section className="checkout-bar" aria-label="Checkout">
      <div>
        <span className="summary-label">Order total</span>
        <strong>{formatMoney(cart.subtotal)}</strong>
      </div>
      <button
        type="button"
        className="checkout-button"
        onClick={onCheckout}
        disabled={!canCheckout || isCheckingOut || isChangingItem}
      >
        {isCheckingOut ? 'Creating order...' : cart.status === 'CHECKED_OUT' ? 'Checked out' : 'Checkout'}
      </button>
    </section>
  );
}
