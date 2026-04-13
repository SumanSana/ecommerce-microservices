import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { AuthService } from '../../services/auth.service';


@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  // Inject your services
  public productService = inject(ProductService);
  public cartService = inject(CartService);
  public authService = inject(AuthService);
  private router = inject(Router);

  /**
   * This updates the global search signal. 
   * The ProductListComponent is listening to this and will re-fetch automatically.
   */
  onSearch(event: any): void {
    const value = event.target.value;
    this.productService.searchTerm.set(value);
    
    // If user is on a different page (like profile/checkout), navigate back to home
    if (this.router.url !== '/') {
      this.router.navigate(['/']);
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}