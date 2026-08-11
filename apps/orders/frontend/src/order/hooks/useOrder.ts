import { useEffect, useMemo, useState } from 'react';
import { fetchOrder } from '../api/ordersApi';
import { PROCESSING_ORDER_STATUSES, type Order } from '../model/Order';

export function useOrder() {
  const orderId = useMemo(
    () => new URLSearchParams(window.location.search).get('orderId'),
    [],
  );
  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(orderId));
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderId) {
      return;
    }

    let cancelled = false;
    let pollTimer: number | undefined;

    async function loadOrder() {
      try {
        const response = await fetchOrder(orderId!);
        if (cancelled) {
          return;
        }
        setOrder(response);
        setError(null);
        setIsLoading(false);
        if (PROCESSING_ORDER_STATUSES.has(response.status)) {
          pollTimer = window.setTimeout(loadOrder, 1000);
        }
      } catch (requestError) {
        if (!cancelled) {
          setError(requestError instanceof Error ? requestError.message : 'Could not load order');
          setIsLoading(false);
        }
      }
    }

    loadOrder();
    return () => {
      cancelled = true;
      window.clearTimeout(pollTimer);
    };
  }, [orderId]);

  const itemCount = order?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  return { orderId, order, itemCount, isLoading, error };
}
