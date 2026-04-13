import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';
import { ProductVariantView, ProductView } from '../../../models/product.view.model';

@Component({
  selector: 'app-update-product',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './update-product.component.html',
  styleUrls: ['./update-product.component.scss']
})
export class UpdateProductComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private productService = inject(ProductService);

  productForm!: FormGroup;
  productId!: string;
  selectedFiles: (File | null)[] = []; 
  previews: (string | null)[] = [];
  isSubmitting = false;

  brands = signal<any[]>([]);
  categories = signal<any[]>([]);

  ngOnInit(): void {
    this.productId = this.route.snapshot.params['id'];
    this.initForm();
    this.loadLookupData();
    this.loadProduct();
  }

  initForm() {
    this.productForm = this.fb.group({
      name: ['', Validators.required],
      brandName: ['', Validators.required],
      categoryName: ['', Validators.required],
      status: ['ACTIVE', Validators.required],
      description: ['', Validators.required],
      variants: this.fb.array([])
    });
  }

  loadLookupData() {
    this.productService.getBrands().subscribe(data => this.brands.set(data));
    this.productService.getLeafCategories().subscribe(data => this.categories.set(data));
  }

  get variants() {
    return this.productForm.get('variants') as FormArray;
  }

  loadProduct() {
    this.productService.getProductById(this.productId).subscribe({
      next: (product: any) => {
        this.productForm.patchValue({
          name: product.name,
          brandName: product.brandName,
          categoryName: product.categoryName,
          status: product.status || 'ACTIVE',
          description: product.description
        });

        this.variants.clear();
        this.selectedFiles = [];
        this.previews = [];

        product.variants.forEach((v: ProductVariantView) => {
          this.addVariant(v);
        });
      }
    });
  }

  addVariant(v?: any) {
    const group = this.fb.group({
      skuId: [v?.skuId || '', Validators.required],
      price: [v?.price || 0, [Validators.required, Validators.min(0)]],
      status: [v?.status || 'ACTIVE', Validators.required],
      imageUrl: [v?.imageUrl || ''],
      attributes: [v?.attributes || {}], // Stays as a JSON object
      isNew: [!v] 
    });
    this.variants.push(group);
    this.selectedFiles.push(null);
    this.previews.push(null);
  }

  // --- ATTRIBUTE HELPERS ---
  getAttributeKeys(index: number): string[] {
    const attributes = this.variants.at(index).get('attributes')?.value;
    return attributes ? Object.keys(attributes) : [];
  }

  addAttribute(index: number) {
    const variantGroup = this.variants.at(index);
    const currentAttrs = { ...variantGroup.get('attributes')?.value };
    const newKey = `Property_${Object.keys(currentAttrs).length + 1}`;
    currentAttrs[newKey] = '';
    variantGroup.get('attributes')?.setValue(currentAttrs);
  }

  removeAttribute(vIndex: number, key: string) {
    const variantGroup = this.variants.at(vIndex);
    const currentAttrs = { ...variantGroup.get('attributes')?.value };
    delete currentAttrs[key];
    variantGroup.get('attributes')?.setValue(currentAttrs);
  }

  updateAttribute(vIndex: number, oldKey: string, newKey: string, value: any) {
    const variantGroup = this.variants.at(vIndex);
    const currentAttrs = { ...variantGroup.get('attributes')?.value };

    if (oldKey !== newKey) {
      delete currentAttrs[oldKey];
    }
    currentAttrs[newKey] = value;
    variantGroup.get('attributes')?.setValue(currentAttrs);
  }

  onFileSelected(event: any, index: number) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFiles[index] = file;
      const reader = new FileReader();
      reader.onload = () => this.previews[index] = reader.result as string;
      reader.readAsDataURL(file);
    }
  }

  onSubmit() {
    if (this.productForm.invalid) return;

    const formValue = this.productForm.getRawValue();
    const brand = this.brands().find(b => b.name.toLowerCase() === formValue.brandName.toLowerCase());
    const cat = this.categories().find(c => c.name.toLowerCase() === formValue.categoryName.toLowerCase());

    if (!brand || !cat) {
      if (!brand) this.productForm.get('brandName')?.setErrors({ invalidSelection: true });
      if (!cat) this.productForm.get('categoryName')?.setErrors({ invalidSelection: true });
      return;
    }

    this.isSubmitting = true;

    const productData = {
      name: formValue.name,
      brandId: brand.id,
      categoryId: cat.id,
      status: formValue.status,
      description: formValue.description,
      variants: formValue.variants.map((v: any) => ({
        skuId: v.skuId,
        price: v.price,
        status: v.status,
        attributes: v.attributes, // Already updated via helper methods
        imageUrl: v.imageUrl 
      }))
    };

    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(productData)], { type: 'application/json' }));

    this.selectedFiles.forEach(file => {
      formData.append('images', file ? file : new Blob([], { type: 'application/octet-stream' }));
    });

    this.productService.updateProduct(this.productId, formData).subscribe({
      next: () => {
        alert('Product updated successfully!');
        this.router.navigate(['/admin/manage-product']);
      },
      error: (err) => {
        console.error(err);
        this.isSubmitting = false;
      }
    });
  }
}