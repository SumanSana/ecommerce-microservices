import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { loadStripe, Stripe, StripeCardElement } from '@stripe/stripe-js';
import { OrderService } from '../../services/order.service';
import { CartService } from '../../services/cart.service';
import { OrderRequest } from '../../models/order.model';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.scss']
})
export class CheckoutComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private orderService = inject(OrderService);
  public cartService = inject(CartService);
  public authService = inject(AuthService);


  // Stripe handles
  private stripePromise = loadStripe(environment.stripePublicKey);
  private stripe?: Stripe;
  private cardElement?: StripeCardElement;

  isProcessing = signal(false);
  cardError = signal<string | null>(null);

  checkoutForm = this.fb.group({
    customerEmail: ['', [Validators.required, Validators.email]],
    shippingAddressLine1: ['', Validators.required],
    shippingCity: ['', Validators.required],
    shippingZipCode: ['', Validators.required],
    shippingCountry: ['India', Validators.required],
  });

  async ngOnInit() {
    this.stripe = await this.stripePromise ?? undefined;
    
    if (this.stripe) {
      const elements = this.stripe.elements();
      this.cardElement = elements.create('card', {
        hidePostalCode: true, // We are collecting Zip Code in our own form
        style: {
          base: {
            fontSize: '16px',
            color: '#212529',
            fontFamily: 'Inter, system-ui, sans-serif',
            '::placeholder': { color: '#6c757d' },
          },
        },
      });
      this.cardElement.mount('#card-element');
      
      // Real-time validation listener
      this.cardElement.on('change', (event) => {
        this.cardError.set(event.error ? event.error.message : null);
      });
    }
  }

  async onSubmit() {
    if (this.checkoutForm.invalid || this.cartService.cartItems().length === 0) return;

    this.isProcessing.set(true);

    let userId:any =   this.authService.currentUser()?.id;
    // 1. Map form to your OrderRequest Record
    const orderRequest: OrderRequest = {
      customerId: userId, // Update this with actual auth ID later
      customerEmail: this.checkoutForm.value.customerEmail!,
      shippingAddressLine1: this.checkoutForm.value.shippingAddressLine1!,
      shippingCity: this.checkoutForm.value.shippingCity!,
      shippingZipCode: this.checkoutForm.value.shippingZipCode!,
      shippingCountry: this.checkoutForm.value.shippingCountry!,
      items: this.cartService.cartItems().map(item => ({
        skuId: item.skuId,
        quantity: item.quantity
      }))
    };

    // 2. Place Order & Get Intent ID (Secret) from Backend
    this.orderService.placeOrder(orderRequest).subscribe({
      next: async (orderResponse) => {
        
        // 3. Confirm the payment with Stripe directly
        // orderResponse.paymentIntentId is the "client_secret" from your Java backend
        const { error, paymentIntent } = await this.stripe!.confirmCardPayment(
          orderResponse.paymentIntentId, {
            payment_method: {
              card: this.cardElement!,
              billing_details: {
                email: this.checkoutForm.value.customerEmail!,
                address: {
                  line1: this.checkoutForm.value.shippingAddressLine1!,
                  city: this.checkoutForm.value.shippingCity!,
                  postal_code: this.checkoutForm.value.shippingZipCode!,
                  country: 'IN' 
                }
              }
            }
          }
        );

        if (error) {
          this.cardError.set(error.message ?? 'Payment failed. Please try again.');
          this.isProcessing.set(false);
        } else if (paymentIntent.status === 'succeeded') {
          // 4. Success! Clear cart and move on
          this.cartService.clearCart();
          this.router.navigate(['/order-success'], { 
            queryParams: { orderId: orderResponse.orderId } 
          });
        }
      },
      error: (err) => {
        console.error('Backend Error:', err);
        this.isProcessing.set(false);
        this.router.navigate(['/order-failed']);
      }
    });
  }
}