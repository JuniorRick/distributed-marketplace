import type { Cart } from '../model/Cart';
import { CartItemRow } from './CartItemRow';

type CartItemListProps = {
  cart: Cart;
  changingItemId: string | null;
  isCheckingOut: boolean;
  onQuantityChange: (itemId: string, quantity: number) => void;
  onRemove: (itemId: string) => void;
};

export function CartItemList({
  cart,
  changingItemId,
  isCheckingOut,
  onQuantityChange,
  onRemove,
}: CartItemListProps) {
  return (
    <section className="cart-items" aria-label="Cart items">
      {cart.items.map((item) => (
        <CartItemRow
          key={item.id}
          item={item}
          disabled={
            changingItemId === item.id ||
            isCheckingOut ||
            cart.status !== 'ACTIVE'
          }
          onQuantityChange={onQuantityChange}
          onRemove={onRemove}
        />
      ))}
    </section>
  );
}
