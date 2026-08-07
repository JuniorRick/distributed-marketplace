import type { OrderItem } from '../model/Order';
import { OrderItemRow } from './OrderItemRow';

type OrderItemListProps = {
  items: OrderItem[];
};

export function OrderItemList({ items }: OrderItemListProps) {
  return (
    <section className="order-items" aria-label="Order items">
      {items.map((item) => <OrderItemRow key={item.id} item={item} />)}
    </section>
  );
}
