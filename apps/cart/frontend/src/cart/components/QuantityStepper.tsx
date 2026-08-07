type QuantityStepperProps = {
  productName: string;
  quantity: number;
  disabled: boolean;
  onChange: (quantity: number) => void;
};

export function QuantityStepper({ productName, quantity, disabled, onChange }: QuantityStepperProps) {
  return (
    <div className="quantity-stepper" aria-label={`Quantity for ${productName}`}>
      <button
        type="button"
        onClick={() => onChange(quantity - 1)}
        disabled={disabled || quantity === 1}
        aria-label={`Decrease ${productName} quantity`}
      >
        -
      </button>
      <output aria-live="polite">{quantity}</output>
      <button
        type="button"
        onClick={() => onChange(quantity + 1)}
        disabled={disabled || quantity === 99}
        aria-label={`Increase ${productName} quantity`}
      >
        +
      </button>
    </div>
  );
}
