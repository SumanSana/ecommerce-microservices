export interface UserRegistration {
  firstName: string;
  lastName: string;
  email: string;
  mobile: string;
  password: string;
}

export interface UserLogin {
  email: string;
  password: string;
}

export interface AuthResponse {
  id: any;
  firstName: string;
  lastName: string;
  mobile: string;
  email: string;
  role: string;
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;         // Duration in seconds
  refresh_expires_in: number; // Duration in seconds
}

export interface UserProfile {
  firstName: string;
  lastName: string;
  email: string;
  mobile: string;
}

export interface PasswordUpdateRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}