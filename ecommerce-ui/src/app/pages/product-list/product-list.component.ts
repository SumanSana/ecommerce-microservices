import { Component, inject, signal, computed, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap, tap } from 'rxjs';

import { ProductService } from '../../services/product.service';
import { ProductView, ProductSearchCriteria } from '../../models/product.view.model';
import { CartService } from '../../services/cart.service';
import { CartItem } from '../../models/cart.model';
import { Brand, Category } from '../../models/product.model';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterModule], 
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss'
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);

  // Data Signals
  products = signal<ProductView[]>([]);
  brands = signal<Brand[]>([]);
  categories = signal<Category[]>([]);
  
  // UI State Signals
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  // Local Filter Signals
  selectedCategory = signal<string>('');
  selectedBrand = signal<string>('');

  // Combined Criteria: Automatically reacts to Navbar search + Sidebar filters
  searchCriteria = computed((): Partial<ProductSearchCriteria> => ({
    searchTerm: this.productService.searchTerm(),
    categoryName: this.selectedCategory(),
    brandName: this.selectedBrand()
  }));

  constructor() {
    toObservable(this.searchCriteria)
      .pipe(
        takeUntilDestroyed(),
        tap(() => {
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap((criteria: Partial<ProductSearchCriteria>) => {
          if (!isPlatformBrowser(this.platformId)) return of([]);
          
          return this.productService.searchProducts(criteria).pipe(
            catchError((err: any) => {
              console.error('Search Error:', err);
              this.error.set('Could not load products. Please try again.');
              return of([]);
            })
          );
        })
      )
      .subscribe((data: ProductView[]) => {
        this.products.set(data);
        this.loading.set(false);
      });
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.loadFilters();
    }
  }

  loadFilters(): void {
    this.productService.getBrands().subscribe({
      next: (data) => this.brands.set(data),
      error: () => console.error('Failed to load brands')
    });

    this.productService.getLeafCategories().subscribe({
      next: (data) => this.categories.set(data),
      error: () => console.error('Failed to load categories')
    });
  }

  filterByCategory(catName: string): void {
    this.selectedCategory.set(this.selectedCategory() === catName ? '' : catName);
  }

  filterByBrand(brandName: string): void {
    this.selectedBrand.set(this.selectedBrand() === brandName ? '' : brandName);
  }

  addVariantToCart(product: ProductView, variant: any, event: Event): void {
  // Prevent the card's click event (viewDetails) from firing
  event.stopPropagation();
  
  if (!variant) return;

  const item: CartItem = {
    productId: product.productId,
    skuId: variant.skuId, // Cart now tracks the specific variant
    name: product.name,
    price: variant.price,
    imageUrl: variant.imageUrl || 'assets/products/placeholder.png',
    quantity: 1
  };

  this.cartService.addToCart(item);
  
  // Optional: Visual feedback
  console.log(`Successfully added variant ${variant.skuId} to cart.`);
}

  viewDetails(productId: string): void {
    this.router.navigate(['/product', productId]);
  }

  refreshList(): void {
    this.productService.searchTerm.set('');
    this.selectedCategory.set('');
    this.selectedBrand.set('');
  }
}