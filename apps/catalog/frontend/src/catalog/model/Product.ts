export type Money = {
  amount: number;
  currency: string;
};

export type Product = {
  id: string;
  sku: string;
  name: string;
  description: string;
  price: Money;
  status: 'ACTIVE' | 'ARCHIVED';
  quantity: number;
};
