import { Routes } from '@angular/router';
import { ProductListComponent } from './pages/product-list/product-list.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { CartComponent } from './pages/cart/cart.component';
import { InventoryComponent } from './pages/admin/inventory/inventory.component';
import { CheckoutComponent } from './pages/checkout/checkout.component';
import { OrderSuccessComponent } from './pages/order-success/order-success.component';
import { adminGuard } from './guards/admin.guard';
import { UnauthorizedComponent } from './pages/unauthorized/unauthorized.component';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout.component';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { ManageProductComponent } from './pages/admin/manage-product/manage-product.component';
import { UpdateProductComponent } from './pages/admin/update-product/update-product.component';
import { MyOrderComponent } from './pages/my-order/my-order.component';
import { OrderListComponent } from './pages/admin/order-list/order-list.component';
import { CreateProductComponent } from './pages/admin/create-product/create-product.component';
import { authGuard } from './guards/auth.guard';
import { OrderFailedComponent } from './pages/order-failed/order-failed.component';


export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      // --- PUBLIC ---a
      { path: 'login', component: LoginComponent, title: 'Login' },
      { path: 'register', component: RegisterComponent, title: 'Register' },

      // --- PROTECTED (Requires Login) ---
      { path: '', component: ProductListComponent, canActivate: [authGuard], title: 'FootKart - Shop' },
      { path: 'cart', component: CartComponent, canActivate: [authGuard], title: 'Your Cart' },
      { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard], title: 'Checkout' },
      { path: 'order-success', component: OrderSuccessComponent, canActivate: [authGuard] },
      { path: 'order-failed', component: OrderFailedComponent, canActivate: [authGuard] },
      { path: 'my-orders', component: MyOrderComponent, canActivate: [authGuard], title: 'My Orders' },
      { path: 'profile', component: ProfileComponent, canActivate: [authGuard], title: 'My Profile' }
    ]
  },

  // ADMIN ROUTES
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: 'manage-product', component: ManageProductComponent, title: 'Admin - Product' },
      { path: 'create-product', component: CreateProductComponent, title: 'Admin - Create Product' },
      { path: 'edit-product/:id', component: UpdateProductComponent, title: 'Admin - Edit Product' },
      { path: 'inventory', component: InventoryComponent, title: 'Admin - Inventory' },
      {path : 'order-track', component: OrderListComponent, title: 'Admin - All Orders'},
      { path: '', component: InventoryComponent, title: 'Admin - Inventory' },
    ]
  },

  { path: 'unauthorized', component: UnauthorizedComponent },
  { path: '**', redirectTo: '' } // Catch-all redirect to home
];