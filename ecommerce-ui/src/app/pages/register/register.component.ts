import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserRegistration } from '../../models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  errorMessage = '';
  loading = false;

  registerForm = this.fb.group({
    // Use 'nonNullable: true' to match your model's 'string' type
    firstName: this.fb.control('', { validators: [Validators.required], nonNullable: true }),
    lastName: this.fb.control('', { validators: [Validators.required], nonNullable: true }),
    email: this.fb.control('', { validators: [Validators.required, Validators.email], nonNullable: true }),
    mobile: this.fb.control('', { validators: [Validators.required], nonNullable: true }),
    password: this.fb.control('', { validators: [Validators.required, Validators.minLength(6)], nonNullable: true }),
    confirmPassword: this.fb.control('', { validators: [Validators.required], nonNullable: true })
  }, { validators: this.passwordMatchValidator });

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const { confirmPassword, ...userData } = this.registerForm.getRawValue()
    const userPayload = userData as UserRegistration;
    this.authService.register(userData).subscribe({
      next: () => {
        this.router.navigate(['/login'], { queryParams: { registered: true } });
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Registration failed. Try again.';
        this.loading = false;
      }
    });
  }
}