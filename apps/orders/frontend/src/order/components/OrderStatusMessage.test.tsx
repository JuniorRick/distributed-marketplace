import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { Order } from '../model/Order';
import { OrderStatusMessage } from './OrderStatusMessage';

const order: Order = {
  id: 'e6caf4e5-3572-4a5d-909f-4334442ee99b',
  sourceCartId: '9b4d2d24-27fc-4a72-a471-7b5c4b69d3b7',
  customerId: '8f8fab76-3d30-4cd5-a8f5-8df79270a330',
  status: 'PENDING',
  failureReason: null,
  items: [],
  total: { amount: 0, currency: 'USD' },
  createdAt: '2026-08-07T08:00:00Z',
};

describe('OrderStatusMessage', () => {
  it('shows that pending checkout is being processed', () => {
    render(<OrderStatusMessage order={order} />);

    expect(screen.getByText('Checkout is being processed...')).toBeInTheDocument();
  });

  it('shows the rejection reason', () => {
    render(
      <OrderStatusMessage
        order={{ ...order, status: 'REJECTED', failureReason: 'Insufficient inventory' }}
      />,
    );

    expect(screen.getByText('Checkout failed: Insufficient inventory')).toBeInTheDocument();
  });

  it('renders no status message for a confirmed order', () => {
    const { container } = render(
      <OrderStatusMessage order={{ ...order, status: 'CONFIRMED' }} />,
    );

    expect(container).toBeEmptyDOMElement();
  });
});
