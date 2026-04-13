import { Component, inject, OnInit, signal, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private platformId = inject(PLATFORM_ID);

  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  // Separated State Signals
  profileLoading = signal(false);
  profileMessage = signal({ type: '', text: '' });

  passwordLoading = signal(false);
  passwordMessage = signal({ type: '', text: '' });

  ngOnInit() {
    this.initForms();
    if (isPlatformBrowser(this.platformId)) {
      // Ensure this runs slightly after init if your authService relies on async data/signals
      setTimeout(() => this.loadUserData());
    }
  }

  private initForms() {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
      mobile: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]]
    });

    // Strong password: Min 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
    const strongPasswordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.pattern(strongPasswordRegex)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  private loadUserData() {
    const profile = this.authService.getProfile();
    if (profile) {
      this.profileForm.patchValue({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        email: profile.email || '',
        mobile: profile.mobile || ''
      });
    }
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPass = control.get('newPassword')?.value;
    const confirmPass = control.get('confirmPassword')?.value;
    
    // Don't show mismatch error if fields are completely empty
    if (!newPass || !confirmPass) return null;
    return newPass === confirmPass ? null : { mismatch: true };
  }

  onUpdateProfile() {
    if (this.profileForm.invalid) return;
    this.profileLoading.set(true);
    this.profileMessage.set({ type: '', text: '' });

    this.authService.updateProfile(this.profileForm.getRawValue()).subscribe({
      next: (res: any) => {
        this.authService.updateLocalUserData(res);
        this.profileMessage.set({ type: 'success', text: 'Profile updated successfully!' });
        this.profileLoading.set(false);
      },
      error: (err) => {
        this.profileMessage.set({ type: 'danger', text: err.error?.message || 'Update failed.' });
        this.profileLoading.set(false);
      }
    });
  }

  onUpdatePassword() {
    if (this.passwordForm.invalid) {
      // Force UI to show errors if they click submit while invalid
      this.passwordForm.markAllAsTouched();
      return;
    }
    
    this.passwordLoading.set(true);
    this.passwordMessage.set({ type: '', text: '' });

    const payload = { ...this.passwordForm.value, email: this.authService.currentUser()?.email };

    this.authService.updatePassword(payload).subscribe({
      next: (res: any) => {
        this.passwordMessage.set({ type: 'success', text: res.message || 'Password updated!' });
        this.passwordLoading.set(false);
        setTimeout(() => this.passwordForm.reset(), 100);
      },
      error: (err) => {
        const errMsg = err.error?.message || 'Failed to update password.';
        this.passwordMessage.set({ type: 'danger', text: errMsg });
        this.passwordLoading.set(false);
      }
    });
  }
}