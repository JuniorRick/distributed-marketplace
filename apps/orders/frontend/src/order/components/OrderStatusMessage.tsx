import { PROCESSING_ORDER_STATUSES, type Order } from '../model/Order';

type OrderStatusMessageProps = {
  order: Order | null;
};

export function OrderStatusMessage({ order }: OrderStatusMessageProps) {
  if (order && PROCESSING_ORDER_STATUSES.has(order.status)) {
    return <p className="state-message processing">Checkout is being processed...</p>;
  }

  if (order?.status === 'REFUNDED') {
    return (
      <p className="state-message error">
        Order cancelled and payment refunded: {order.failureReason ?? 'Inventory could not be committed'}
      </p>
    );
  }

  if (order?.status === 'MANUAL_REVIEW') {
    return (
      <p className="state-message error">
        Checkout requires manual review: {order.failureReason ?? 'Automatic recovery was exhausted'}
      </p>
    );
  }

  if (order?.status === 'REJECTED') {
    return (
      <p className="state-message error">
        Checkout failed: {order.failureReason ?? 'Cart rejected the checkout request'}
      </p>
    );
  }

  return null;
}
