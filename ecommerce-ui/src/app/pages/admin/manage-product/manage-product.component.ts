import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { ProductService } from '../../../services/product.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductView } from '../../../models/product.view.model';

@Component({
  selector: 'app-manage-product',
  standalone: true, // Ensure this is set to true
  imports: [
    CommonModule,   // Required for *ngFor, *ngIf, [class]
    FormsModule,    // Required for [(ngModel)]
    RouterModule    // Required for routerLink
  ],
  templateUrl: './manage-product.component.html',
  styleUrls: ['./manage-product.component.scss']
})
export class ManageProductComponent implements OnInit {
  products: any[] = [];
  filteredProducts: any[] = [];
  searchTerm: string = '';
  selectedStatus: string = '';

  constructor(
    private productService: ProductService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (data: ProductView[]) => {
        // Data is already typed, but we ensure the filtered list is initialized
        this.products = data;
        this.filteredProducts = [...data];
      },
      error: (err) => console.error('Error fetching products', err)
    });
  }

  applyFilters(): void {
    this.filteredProducts = this.products.filter(product => {
      // Corrected to use productId and existing brandName/categoryName from your DTO
      const matchesSearch =
        product.name.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        product.brandName.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesStatus = this.selectedStatus ? product.status === this.selectedStatus : true;

      return matchesSearch && matchesStatus;
    });
  }

  editProduct(productId: string): void {
    // Ensure we are passing the string ID to the router
    this.router.navigate(['/admin/edit-product', productId]);
  }
}