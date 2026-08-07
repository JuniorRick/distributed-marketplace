import { formatMoney } from '../../shared/formatMoney';
import type { CartItem } from '../model/Cart';
import { QuantityStepper } from './QuantityStepper';

type CartItemRowProps = {
  item: CartItem;
  disabled: boolean;
  onQuantityChange: (itemId: string, quantity: number) => void;
  onRemove: (itemId: string) => void;
};

export function CartItemRow({ item, disabled, onQuantityChange, onRemove }: CartItemRowProps) {
  return (
    <article className="cart-item">
      <div>
        <span className="cart-item__sku">{item.productSku}</span>
        <h2>{item.productName}</h2>
        <span className="cart-item__unit-price">{formatMoney(item.unitPrice)} each</span>
      </div>
      <div className="cart-item__actions">
        <QuantityStepper
          productName={item.productName}
          quantity={item.quantity}
          disabled={disabled}
          onChange={(quantity) => onQuantityChange(item.id, quantity)}
        />
        <button
          className="remove-item-button"
          type="button"
          onClick={() => onRemove(item.id)}
          disabled={disabled}
        >
          Remove
        </button>
      </div>
      <strong className="cart-item__total">{formatMoney(item.lineTotal)}</strong>
    </article>
  );
}
