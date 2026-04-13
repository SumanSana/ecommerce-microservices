import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { InventoryService } from '../../../services/inventory.service';
import { InventoryTransaction } from '../../../models/inventory.model';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './inventory.component.html'
})
export class InventoryComponent implements OnInit {
  private fb = inject(FormBuilder);
  private inventoryService = inject(InventoryService);

  inventoryForm = this.fb.group({
    skuId: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(1)]],
    transactionType: ['INBOUND', Validators.required],
    comment: ['']
  });

  skuSuggestions = signal<string[]>([]);
  availableQuantity = signal<number | null>(null); // New signal for stock display
  isDropdownOpen = signal(false);
  isSubmitting = signal(false);
  successMessage = signal<string | null>(null);

  ngOnInit() {
    this.inventoryForm.get('skuId')?.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term => {
        // If user clears the input, reset stock display and suggestions
        if (!term) {
          this.availableQuantity.set(null);
          this.skuSuggestions.set([]);
          return of([]);
        }
        return this.inventoryService.searchSkus(term);
      })
    ).subscribe(res => {
      this.skuSuggestions.set(res);
      this.isDropdownOpen.set(true);
    });
  }

  onInputClick() {
    this.isDropdownOpen.set(true);
    if (this.skuSuggestions().length === 0) {
      this.inventoryService.searchSkus('').subscribe(res => this.skuSuggestions.set(res));
    }
  }

  selectSku(sku: string) {
    this.inventoryForm.get('skuId')?.setValue(sku, { emitEvent: false });
    this.isDropdownOpen.set(false);
    
    // Fetch the available quantity for the selected SKU
    this.inventoryService.getAvailableQuantity(sku).subscribe({
      next: (qty) => this.availableQuantity.set(qty),
      error: () => this.availableQuantity.set(0) // Default to 0 if error occurs
    });
  }

  onSubmit() {
    if (this.inventoryForm.invalid) return;
    this.isSubmitting.set(true);

    const payload = this.inventoryForm.value as InventoryTransaction;

    this.inventoryService.updateStock(payload).subscribe({
      next: () => {
        this.successMessage.set(`Successfully updated ${payload.skuId}`);
        this.inventoryForm.reset({ 
          transactionType: 'INBOUND', 
          quantity: 1, 
          skuId: '', 
          comment: '' 
        });
        
        // Clear state after success
        this.availableQuantity.set(null); 
        this.isSubmitting.set(false);
        this.isDropdownOpen.set(false);
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: () => this.isSubmitting.set(false)
    });
  }
}