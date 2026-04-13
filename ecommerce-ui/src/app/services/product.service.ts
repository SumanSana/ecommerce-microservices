import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable, of } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { PageResponse, ProductSearchCriteria, ProductView } from '../models/product.view.model';
import { Brand, Category } from '../models/product.model';
import { Constants } from '../constants/constant';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  searchTerm = signal<string>('');

  getAllProducts(): Observable<ProductView[]> {
    return this.http.get<ProductView[]>(Constants.PRODUCT_SERVICE_URL);
  }

  getProductById(id: string): Observable<ProductView> {
    return this.http.get<ProductView>(`${Constants.PRODUCT_SERVICE_URL}/${id}`);
  }

  searchProducts(criteria: Partial<ProductSearchCriteria> = {}): Observable<ProductView[]> {
    if (!isPlatformBrowser(this.platformId)) return of([]);

    let params = new HttpParams();

    if (criteria.searchTerm) params = params.set('searchTerm', criteria.searchTerm);
    if (criteria.brandName) params = params.set('brandName', criteria.brandName);
    if (criteria.categoryName) params = params.set('categoryName', criteria.categoryName);
    if (criteria.inStockOnly !== undefined) params = params.set('inStockOnly', criteria.inStockOnly);
    if (criteria.minPrice) params = params.set('minPrice', criteria.minPrice.toString());
    if (criteria.maxPrice) params = params.set('maxPrice', criteria.maxPrice.toString());

    return this.http.get<PageResponse<ProductView>>(`${Constants.PRODUCT_SERVICE_URL}/search`, { params }).pipe(
      map((response: PageResponse<ProductView>) => response.content || [])
    );
  }

  getBrands(): Observable<Brand[]> {
    if (isPlatformBrowser(this.platformId)) {
      return this.http.get<Brand[]>(`${Constants.PRODUCT_SERVICE_URL}/brands`);
    }
    return of([]);
  }

  getCategories(): Observable<Category[]> {
    if (isPlatformBrowser(this.platformId)) {
      return this.http.get<Category[]>(`${Constants.PRODUCT_SERVICE_URL}/categories`);
    }
    return of([]);
  }

  getLeafCategories(): Observable<Category[]> {
    if (isPlatformBrowser(this.platformId)) {
      return this.http.get<Category[]>(`${Constants.PRODUCT_SERVICE_URL}/leaf-categories`);
    }
    return of([]);
  }

  createProduct(formData: FormData): Observable<any> {
    return this.http.post(Constants.PRODUCT_MANGEMENT_SERVICE_URL, formData);
  }

  updateProduct(id: string, formData: FormData): Observable<any> {
  // Use the full URL to be 100% sure
  return this.http.put(`http://localhost:8080/ekart/v1/products/admin/${id}`, formData);
}
}