import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { InventoryTransaction } from '../models/inventory.model';
import { isPlatformBrowser } from '@angular/common';
import { Constants } from '../constants/constant';


@Injectable({ providedIn: 'root' })
export class InventoryService {
  private platformId = inject(PLATFORM_ID);
  private http = inject(HttpClient);

  // Search SKUs for the autocomplete
  searchSkus(term: string): Observable<string[]> {
    if (!isPlatformBrowser(this.platformId)) return of([]);
    return this.http.get<string[]>(`${Constants.INVENTORY_SERVICE_URL}/skus/search?term=${term}`);
  }

  getAvailableQuantity(skuId: string): Observable<number> {
    return this.http.get<number>(`${Constants.INVENTORY_SERVICE_URL}/${skuId}/availability`);
  }

  // Post the stock update
  updateStock(transaction: InventoryTransaction): Observable<any> {
    return this.http.post(`${Constants.INVENTORY_SERVICE_URL}/transactions`, transaction);
  }

}