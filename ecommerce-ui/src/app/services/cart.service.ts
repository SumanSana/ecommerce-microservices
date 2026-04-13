import { Injectable, signal, effect, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CartItem } from '../models/cart.model';


@Injectable({ providedIn: 'root' })
export class CartService {
  private platformId = inject(PLATFORM_ID);
  
  // The main state: A Signal holding the array of items
  cartItems = signal<CartItem[]>([]);

  constructor() {
    // 1. Load from LocalStorage on startup (Browser only)
    if (isPlatformBrowser(this.platformId)) {
      const savedCart = localStorage.getItem('ekart_cart');
      if (savedCart) {
        this.cartItems.set(JSON.parse(savedCart));
      }

      // 2. Automatically save to LocalStorage whenever the signal changes
      effect(() => {
        localStorage.setItem('ekart_cart', JSON.stringify(this.cartItems()));
      });
    }
  }

  addToCart(item: CartItem) {
    this.cartItems.update(items => {
      const existingItem = items.find(i => i.skuId === item.skuId);
      
      if (existingItem) {
        // Increment quantity if SKU already exists
        return items.map(i => 
          i.skuId === item.skuId ? { ...i, quantity: i.quantity + 1 } : i
        );
      }
      // Add as new item
      return [...items, item];
    });
  }

  // Add these to your existing CartService
updateQuantity(skuId: string, delta: number) {
  this.cartItems.update(items => items.map(item => {
    if (item.skuId === skuId) {
      const newQty = item.quantity + delta;
      return { ...item, quantity: newQty > 0 ? newQty : 1 };
    }
    return item;
  }));
}

clearCart() {
  this.cartItems.set([]);
}

  removeFromCart(skuId: string) {
    this.cartItems.update(items => items.filter(i => i.skuId !== skuId));
  }

  getCartCount() {
    // Returns total number of items (e.g., 2 shirts + 1 hat = 3)
    return this.cartItems().reduce((acc, item) => acc + item.quantity, 0);
  }

  getCartTotal() {
    return this.cartItems().reduce((acc, item) => acc + (item.price * item.quantity), 0);
  }
}