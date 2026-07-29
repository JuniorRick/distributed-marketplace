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

export type Order = {
  id: string;
  sourceCartId: string;
  customerId: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  items: OrderItem[];
  total: Money;
  createdAt: string;
};
