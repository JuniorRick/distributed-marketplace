import { useState } from 'react';
import { createOrderFromCart, orderPageUrl } from '../api/orderApi';

export function useCheckout(
  cartId: string | null,
  canCheckout: boolean,
  onError: (message: string | null) => void,
) {
  const [isCheckingOut, setIsCheckingOut] = useState(false);

  async function checkout() {
    if (!cartId || !canCheckout) {
      return;
    }

    setIsCheckingOut(true);
    onError(null);
    try {
      const order = await createOrderFromCart(cartId);
      window.location.assign(orderPageUrl(order.id));
    } catch (requestError) {
      onError(requestError instanceof Error ? requestError.message : 'Could not create order');
      setIsCheckingOut(false);
    }
  }

  return { isCheckingOut, checkout };
}
