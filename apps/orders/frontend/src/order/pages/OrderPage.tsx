import { OrderHeader } from '../components/OrderHeader';
import { OrderItemList } from '../components/OrderItemList';
import { OrderReference } from '../components/OrderReference';
import { OrderStatusMessage } from '../components/OrderStatusMessage';
import { OrderSummary } from '../components/OrderSummary';
import { useOrder } from '../hooks/useOrder';

export function OrderPage() {
  const { orderId, order, itemCount, isLoading, error } = useOrder();

  return (
    <main className="app-shell">
      <OrderHeader />
      <OrderSummary order={order} itemCount={itemCount} />

      {isLoading && <p className="state-message">Loading order...</p>}
      {error && <p className="state-message error">{error}</p>}
      {!orderId && <p className="state-message">Create an order from Cart to view it here.</p>}
      <OrderStatusMessage order={order} />

      {order && (
        <>
          <OrderReference order={order} />
          <OrderItemList items={order.items} />
        </>
      )}
    </main>
  );
}
