export type Money = {
  amount: number;
  currency: string;
};

export type OrderItem = {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  unitPrice: Money;
  quantity: number;
  lineTotal: Money;
};

export type OrderStatus =
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

export const PROCESSING_ORDER_STATUSES: ReadonlySet<OrderStatus> = new Set([
  'PENDING',
  'CHECKOUT_PENDING',
  'INVENTORY_RESERVATION_PENDING',
  'PAYMENT_PENDING',
  'INVENTORY_COMMIT_PENDING',
  'INVENTORY_RELEASE_PENDING',
  'INVENTORY_RELEASE_FOR_REFUND_PENDING',
  'REFUND_PENDING',
]);

export type Order = {
  id: string;
  sourceCartId: string;
  customerId: string;
  status: OrderStatus;
  failureReason: string | null;
  items: OrderItem[];
  total: Money;
  createdAt: string;
};
