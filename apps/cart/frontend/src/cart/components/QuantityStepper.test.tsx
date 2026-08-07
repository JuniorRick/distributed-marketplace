import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { QuantityStepper } from './QuantityStepper';

describe('QuantityStepper', () => {
  it('requests quantity changes within the allowed range', () => {
    const changeQuantity = vi.fn();
    render(
      <QuantityStepper
        productName="Mechanical Keyboard"
        quantity={2}
        disabled={false}
        onChange={changeQuantity}
      />,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Decrease Mechanical Keyboard quantity' }));
    fireEvent.click(screen.getByRole('button', { name: 'Increase Mechanical Keyboard quantity' }));

    expect(changeQuantity).toHaveBeenNthCalledWith(1, 1);
    expect(changeQuantity).toHaveBeenNthCalledWith(2, 3);
  });

  it('disables controls at the boundaries', () => {
    const { rerender } = render(
      <QuantityStepper
        productName="Mechanical Keyboard"
        quantity={1}
        disabled={false}
        onChange={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: 'Decrease Mechanical Keyboard quantity' }))
      .toBeDisabled();

    rerender(
      <QuantityStepper
        productName="Mechanical Keyboard"
        quantity={99}
        disabled={false}
        onChange={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: 'Increase Mechanical Keyboard quantity' }))
      .toBeDisabled();
  });
});
