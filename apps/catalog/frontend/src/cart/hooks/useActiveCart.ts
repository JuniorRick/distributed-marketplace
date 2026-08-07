import { useEffect, useMemo, useState } from 'react';
import type { Product } from '../../catalog/model/Product';
import { addProductToCart, loadActiveCart } from '../api/cartApi';
import type { Cart } from '../model/Cart';

type CartMessage = {
  text: string;
  error: boolean;
};

export function useActiveCart() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [addingProductId, setAddingProductId] = useState<string | null>(null);
  const [message, setMessage] = useState<CartMessage | null>(null);

  useEffect(() => {
    let cancelled = false;

    loadActiveCart()
      .then((activeCart) => {
        if (!cancelled) {
          setCart(activeCart);
        }
      })
      .catch((requestError: Error) => {
        if (!cancelled) {
          setMessage({ text: requestError.message, error: true });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const itemCount = useMemo(
    () => cart?.items.reduce((total, item) => total + item.quantity, 0) ?? 0,
    [cart],
  );

  async function addToCart(product: Product) {
    setAddingProductId(product.id);
    setMessage(null);
    try {
      setCart(await addProductToCart(product.id));
      setMessage({ text: `${product.name} added to cart.`, error: false });
    } catch (requestError) {
      const errorMessage = requestError instanceof Error
        ? requestError.message
        : 'Could not add product';
      setMessage({ text: errorMessage, error: true });
    } finally {
      setAddingProductId(null);
    }
  }

  return { cart, itemCount, addingProductId, message, addToCart };
}
