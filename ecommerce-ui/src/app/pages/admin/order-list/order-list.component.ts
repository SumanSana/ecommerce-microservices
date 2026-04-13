import { Component, inject, signal } from '@angular/core';
import { OrderService } from '../../../services/order.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Order } from '../../../models/order.model';


@Component({
  selector: 'app-order-list',
  imports: [CommonModule, RouterModule],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss'
})
export class OrderListComponent {
private orderService = inject(OrderService);
  
  orders = signal<Order[]>([]);
  isLoading = signal(true);
  
  // Track which order is expanded to show details
  expandedOrderId = signal<string | null>(null);

  ngOnInit(): void {
    
    this.orderService.getAllOrders().subscribe({
      next: (data:Order[]) => {
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
