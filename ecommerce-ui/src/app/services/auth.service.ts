import { Injectable, inject, signal, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { isPlatformBrowser } from '@angular/common';
import { Observable, tap, of } from 'rxjs';
import { AuthResponse, PasswordUpdateRequest, UserLogin, UserProfile, UserRegistration } from '../models/user.model';
import { Constants } from '../constants/constant';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);

  // Signals for UI reactivity
  currentUser = signal<AuthResponse | null>(this.getUserFromStorage());
  isLoggedIn = signal<boolean>(!!this.getAccessToken());

  /**
   * Register a new user
   */
  register(userData: UserRegistration): Observable<any> {
    return this.http.post(`${Constants.AUTH_SERVICE_URL}/register`, userData);
  }

  /**
   * Login and initialize session
   */
  login(credentials: UserLogin): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${Constants.AUTH_SERVICE_URL}/login`, credentials).pipe(
      tap((response) => this.saveSession(response))
    );
  }

  /**
   * Returns profile data directly from the current signal 
   * to avoid unnecessary network calls.
   */
  getProfile(): UserProfile | null {
    const user = this.currentUser();
    if (!user) return null;

    return {
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      mobile: user.mobile
    };
  }

  /**
   * Updates user profile in the database
   */
  updateProfile(profile: UserProfile): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${Constants.IDENTITY_SERVICE_URL}/profile`, profile);
  }

  /**
   * Changes user password
   */
  updatePassword(passwords: PasswordUpdateRequest): Observable<any> {
    return this.http.post(`${Constants.IDENTITY_SERVICE_URL}/change-password`, passwords);
  }

  /**
   * Updates the stored user data and signal without requiring a re-login.
   * Call this after a successful updateProfile() call.
   */
  updateLocalUserData(updatedProfile: UserProfile): void {
    if (isPlatformBrowser(this.platformId)) {
      const currentData = this.getUserFromStorage();
      if (currentData) {
        // Merge the updated profile fields into the existing AuthResponse
        const newData = { ...currentData, ...updatedProfile };
        localStorage.setItem('user_data', JSON.stringify(newData));
        this.currentUser.set(newData);
      }
    }
  }

  /**
   * Refresh the access token using the stored refresh token
   */
  refreshToken(): Observable<AuthResponse> {
    if (isPlatformBrowser(this.platformId)) {
      const rToken = localStorage.getItem('refresh_token');
      return this.http.post<AuthResponse>(`${Constants.AUTH_SERVICE_URL}/refresh`, rToken).pipe(
        tap((response) => this.saveSession(response))
      );
    }
    return of();
  }

  /**
   * Logout and cleanup
   */
  logout(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.clear();
    }
    this.currentUser.set(null);
    this.isLoggedIn.set(false);
    this.router.navigate(['/login']);
  }

  /**
   * Safely retrieves the access token only in the browser
   */
  getAccessToken(): string | null {
    if (isPlatformBrowser(this.platformId)) {
      return localStorage.getItem('access_token');
    }
    return null;
  }

  /**
   * Checks if the Access Token has passed its absolute expiry timestamp
   */
  isTokenExpired(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      const expiry = localStorage.getItem('at_expiry');
      return !expiry || Date.now() > parseInt(expiry);
    }
    return true;
  }

  /**
   * Checks if the Refresh Token is still valid to attempt a refresh
   */
  isRefreshTokenExpired(): boolean {
    if (isPlatformBrowser(this.platformId)) {
      const expiry = localStorage.getItem('rt_expiry');
      return !expiry || Date.now() > parseInt(expiry);
    }
    return true;
  }

  /**
   * Persists tokens and user data to localStorage with absolute timestamps
   */
  private saveSession(res: AuthResponse): void {
    if (isPlatformBrowser(this.platformId)) {
      const now = Date.now();
      localStorage.setItem('access_token', res.access_token);
      localStorage.setItem('refresh_token', res.refresh_token);
      localStorage.setItem('user_data', JSON.stringify(res));
      localStorage.setItem('at_expiry', (now + res.expires_in * 1000).toString());
      localStorage.setItem('rt_expiry', (now + res.refresh_expires_in * 1000).toString());
    }

    this.currentUser.set(res);
    this.isLoggedIn.set(true);
  }

  /**
   * Initializes the signal from storage on application load
   */
  private getUserFromStorage(): AuthResponse | null {
    if (isPlatformBrowser(this.platformId)) {
      const data = localStorage.getItem('user_data');
      try {
        return data ? JSON.parse(data) : null;
      } catch (e) {
        return null;
      }
    }
    return null;
  }
}