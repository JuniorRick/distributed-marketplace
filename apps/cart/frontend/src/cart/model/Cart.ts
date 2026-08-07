export type Money = {
  amount: number;
  currency: string;
};

export type CartItem = {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  unitPrice: Money;
  quantity: number;
  lineTotal: Money;
};

export type Cart = {
  id: string;
  customerId: string;
  status: 'ACTIVE' | 'CHECKED_OUT' | 'ABANDONED';
  items: CartItem[];
  subtotal: Money;
};
