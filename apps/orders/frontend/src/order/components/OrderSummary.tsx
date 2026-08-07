import { formatMoney } from '../../shared/formatMoney';
import type { Order } from '../model/Order';

type OrderSummaryProps = {
  order: Order | null;
  itemCount: number;
};

export function OrderSummary({ order, itemCount }: OrderSummaryProps) {
  return (
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
  );
}
