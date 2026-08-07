import { useEffect, useMemo, useState } from 'react';
import { fetchCart, removeCartItem, updateCartItemQuantity } from '../api/cartApi';
import type { Cart } from '../model/Cart';

export function useCart() {
  const cartId = useMemo(
    () => new URLSearchParams(window.location.search).get('cartId'),
    [],
  );
  const [cart, setCart] = useState<Cart | null>(null);
  const [isLoading, setIsLoading] = useState(Boolean(cartId));
  const [error, setError] = useState<string | null>(null);
  const [changingItemId, setChangingItemId] = useState<string | null>(null);

  useEffect(() => {
    if (!cartId) {
      return;
    }

    let cancelled = false;
    fetchCart(cartId)
      .then((response) => {
        if (!cancelled) {
          setCart(response);
          setError(null);
        }
      })
      .catch((requestError: Error) => {
        if (!cancelled) {
          setError(requestError.message);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [cartId]);

  const itemCount = cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0;

  async function changeQuantity(itemId: string, quantity: number) {
    if (!cartId) {
      return;
    }

    setChangingItemId(itemId);
    setError(null);
    try {
      setCart(await updateCartItemQuantity(cartId, itemId, quantity));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not change quantity');
    } finally {
      setChangingItemId(null);
    }
  }

  async function removeItem(itemId: string) {
    if (!cartId) {
      return;
    }

    setChangingItemId(itemId);
    setError(null);
    try {
      setCart(await removeCartItem(cartId, itemId));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not remove item');
    } finally {
      setChangingItemId(null);
    }
  }

  return {
    cartId,
    cart,
    itemCount,
    isLoading,
    error,
    setError,
    changingItemId,
    changeQuantity,
    removeItem,
  };
}
