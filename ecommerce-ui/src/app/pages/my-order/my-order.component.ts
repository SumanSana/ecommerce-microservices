import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

@Component({
  selector: 'app-my-order',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-order.component.html',
  styleUrl: './my-order.component.scss'
})
export class MyOrderComponent implements OnInit {
  private orderService = inject(OrderService);
  
  orders = signal<Order[]>([]);
  isLoading = signal(true);
  
  // Track which order is expanded to show details
  expandedOrderId = signal<string | null>(null);

  ngOnInit(): void {
    
    this.orderService.getMyOrders().subscribe({
      next: (data) => {
        this.orders.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error fetching orders:', err);
        this.isLoading.set(false);
      }
    });
  }

  toggleDetails(orderId: string) {
    // If the same ID is clicked, hide it; otherwise, show the new one
    if (this.expandedOrderId() === orderId) {
      this.expandedOrderId.set(null);
    } else {
      this.expandedOrderId.set(orderId);
    }
  }

  getStatusClass(status: string | undefined): string {
    const s = status?.toUpperCase() || 'PENDING';
    switch (s) {
      case 'CONFIRMED': return 'badge bg-success shadow-sm';
      case 'PENDING': return 'badge bg-warning text-dark shadow-sm';
      case 'CANCELLED': return 'badge bg-danger shadow-sm';
      default: return 'badge bg-secondary shadow-sm';
    }
  }
}