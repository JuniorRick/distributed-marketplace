export type Order = {
  id: string;
  sourceCartId: string;
  status:
    | 'PENDING'
    | 'CHECKOUT_PENDING'
    | 'INVENTORY_RESERVATION_PENDING'
    | 'PAYMENT_PENDING'
    | 'INVENTORY_COMMIT_PENDING'
    | 'INVENTORY_RELEASE_PENDING'
    | 'INVENTORY_RELEASE_FOR_REFUND_PENDING'
    | 'REFUND_PENDING'
    | 'CONFIRMED'
    | 'REJECTED'
    | 'REFUNDED'
    | 'MANUAL_REVIEW';
};
