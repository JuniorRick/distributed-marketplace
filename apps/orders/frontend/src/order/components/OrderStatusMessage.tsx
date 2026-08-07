import type { Order } from '../model/Order';

type OrderStatusMessageProps = {
  order: Order | null;
};

export function OrderStatusMessage({ order }: OrderStatusMessageProps) {
  if (order?.status === 'PENDING') {
    return <p className="state-message processing">Checkout is being processed...</p>;
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
