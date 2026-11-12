export type UserScope = 'PLATFORM' | 'HOSPITAL';
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'SUSPENDED';

export interface AuthUser {
  publicId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  userScope: UserScope;
  hospitalId: number | null;
  hospitalCode: string | null;
  hospitalName: string | null;
  roles: string[];
  permissions: string[];
  status: AccountStatus;
  mustChangePassword: boolean;
  lastLoginAt: string | null;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
  rememberMe: boolean;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface AuthTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  csrfToken: string;
  user: AuthUser;
}

export interface UserPermissionsResponse {
  userScope: UserScope;
  hospitalId: number | null;
  roles: string[];
  permissions: string[];
}

export interface CsrfTokenResponse {
  csrfToken: string;
  headerName: string;
}
