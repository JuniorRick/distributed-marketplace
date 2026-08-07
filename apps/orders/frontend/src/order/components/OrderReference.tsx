import type { Order } from '../model/Order';

type OrderReferenceProps = {
  order: Order;
};

export function OrderReference({ order }: OrderReferenceProps) {
  return (
    <section className="order-reference" aria-label="Order reference">
      <span>Order</span>
      <strong>{order.id}</strong>
      <span>{new Date(order.createdAt).toLocaleString()}</span>
    </section>
  );
}
