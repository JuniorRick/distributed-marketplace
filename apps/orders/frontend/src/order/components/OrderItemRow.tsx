import { formatMoney } from '../../shared/formatMoney';
import type { OrderItem } from '../model/Order';

type OrderItemRowProps = {
  item: OrderItem;
};

export function OrderItemRow({ item }: OrderItemRowProps) {
  return (
    <article className="order-item">
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
  );
}
