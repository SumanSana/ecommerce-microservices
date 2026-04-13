import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../../services/product.service';

@Component({
  selector: 'app-create-product',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-product.component.html'
})
export class CreateProductComponent implements OnInit {
  private fb = inject(FormBuilder);
  private productService = inject(ProductService);
  private router = inject(Router);

  productForm: FormGroup;
  isSubmitting = signal(false);
  brands = signal<any[]>([]);
  categories = signal<any[]>([]);

  constructor() {
    this.productForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', Validators.required],
      brandName: ['', Validators.required],
      categoryName: ['', Validators.required],
      variants: this.fb.array([this.createVariant()])
    });
  }

  ngOnInit() {
    // Load lookup data for the datalists
    this.productService.getBrands().subscribe(data => this.brands.set(data));
    this.productService.getLeafCategories().subscribe(data => this.categories.set(data));
  }

  get variants() {
    return this.productForm.get('variants') as FormArray;
  }

  createVariant(): FormGroup {
    return this.fb.group({
      skuId: ['', Validators.required],
      price: [0, [Validators.required, Validators.min(1)]],
      imageFile: [null, Validators.required],
      attributesText: [''] // Format: "Color: Red, Material: Cotton"
    });
  }

  addVariant() {
    this.variants.push(this.createVariant());
  }

  removeVariant(index: number) {
    if (this.variants.length > 1) {
      this.variants.removeAt(index);
    }
  }

  onFileSelected(event: any, index: number) {
    const file = event.target.files[0];
    if (file) {
      this.variants.at(index).patchValue({ imageFile: file });
    }
  }

  private parseAttributes(text: string): Record<string, string> {
    const map: Record<string, string> = {};
    if (!text) return map;
    
    text.split(',').forEach(pair => {
      const [key, value] = pair.split(':');
      if (key && value) {
        map[key.trim()] = value.trim();
      }
    });
    return map;
  }

  onSubmit() {
    if (this.productForm.invalid) return;

    const formValue = this.productForm.value;
    
    // Cross-reference typed names with our UUID-carrying objects
    const brand = this.brands().find(b => b.name.toLowerCase() === formValue.brandName.toLowerCase());
    const cat = this.categories().find(c => c.name.toLowerCase() === formValue.categoryName.toLowerCase());

    if (!brand || !cat) {
      if (!brand) this.productForm.get('brandName')?.setErrors({ invalidSelection: true });
      if (!cat) this.productForm.get('categoryName')?.setErrors({ invalidSelection: true });
      return;
    }

    this.isSubmitting.set(true);

    // Prepare JSON DTO
    const productData = {
      name: formValue.name,
      description: formValue.description,
      brandId: brand.id,
      categoryId: cat.id,
      variants: formValue.variants.map((v: any) => ({
        skuId: v.skuId,
        price: v.price,
        attributes: this.parseAttributes(v.attributesText)
      }))
    };

    const formData = new FormData();
    // Wrap JSON in a Blob so Spring Boot identifies it as application/json
    formData.append('product', new Blob([JSON.stringify(productData)], { type: 'application/json' }));
    
    // Append images
    formValue.variants.forEach((v: any) => {
      if (v.imageFile) {
        formData.append('images', v.imageFile);
      }
    });

    this.productService.createProduct(formData).subscribe({
      next: () => this.router.navigate(['/admin/products']),
      error: () => this.isSubmitting.set(false)
    });
  }
}