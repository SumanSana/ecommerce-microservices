import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './cart.component.html'
})
export class CartComponent {
  public cartService = inject(CartService);
  private router = inject(Router);

  onCheckout() {
    // This is where you will eventually integrate Stripe
    console.log('Proceeding to checkout with:', this.cartService.cartItems());
    this.router.navigate(['/checkout']);
  }
}