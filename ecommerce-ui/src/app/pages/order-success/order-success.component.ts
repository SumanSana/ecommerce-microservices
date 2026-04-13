import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-success.component.html',
  styleUrls: ['./order-success.component.scss']
})
export class OrderSuccessComponent implements OnInit {
  private route = inject(ActivatedRoute);
  orderId = signal<string | null>(null);

  ngOnInit(): void {
    // Correctly accessing the param passed from CheckoutComponent
    this.route.queryParams.subscribe(params => {
      this.orderId.set(params['orderId']);
    });
  }
}