export interface OrderItem {
  skuId: string;
  quantity: number;
  price: number;
}

export interface Order {
  id:string
  customerId:string;
  customerEmail:string,
  shippingAddressLine1: string;
  shippingCity: string;
  shippingZipCode: string;
  shippingCountry: string;
  totalAmount: number;
  items: OrderItem[];
  status:string,
  createdAt: Date,
  updatedAt: Date
}


export interface OrderItemRequest {
  skuId: string;
  quantity: number;
}

export interface OrderRequest {
  customerId:string;
  customerEmail:string,
  items: OrderItemRequest[];
  shippingAddressLine1: string;
  shippingCity: string;
  shippingZipCode: string;
  shippingCountry: string;
}

export interface OrderResponse {
  orderId: string;
  status: string; // PENDING, CONFIRMED, FAILED
  totalAmount: number;
  paymentIntentId: string; // Stripe Hosted Checkout URL
}