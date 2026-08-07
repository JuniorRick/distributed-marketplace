import { formatMoney } from '../../shared/formatMoney';
import type { Cart } from '../model/Cart';

type CartSummaryProps = {
  cart: Cart | null;
  itemCount: number;
};

export function CartSummary({ cart, itemCount }: CartSummaryProps) {
  return (
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
  );
}
