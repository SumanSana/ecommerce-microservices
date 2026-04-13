export interface InventoryTransaction {
  skuId: string;
  quantity: number;
  transactionType: 'INBOUND' | 'OUTBOUND';
  comment: string;
}