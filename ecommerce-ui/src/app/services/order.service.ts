import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, OrderRequest, OrderResponse } from '../models/order.model';
import { Constants } from '../constants/constant';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);


  placeOrder(request: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>(`${Constants.ORDER_SERVICE_URL}`, request);
  }

  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${Constants.ORDER_SERVICE_URL}/customer/${this.authService.currentUser()?.id}`);
  }

  getAllOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${Constants.ORDER_SERVICE_URL}`);
  }
}