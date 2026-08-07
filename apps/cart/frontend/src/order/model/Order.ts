export type Order = {
  id: string;
  sourceCartId: string;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED';
};
