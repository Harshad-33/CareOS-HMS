import { Injectable, computed, signal } from '@angular/core';
import { AuthUser, UserScope } from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthStateService {
  // Short-lived access token and CSRF token reside strictly in memory (never browser storage).
  private readonly accessTokenSignal = signal<string | null>(null);
  private readonly csrfTokenSignal = signal<string | null>(null);
  private readonly userSignal = signal<AuthUser | null>(null);
  private readonly initializedSignal = signal<boolean>(false);
  private readonly sessionExpiredMessageSignal = signal<string | null>(null);

  readonly accessToken = this.accessTokenSignal.asReadonly();
  readonly csrfToken = this.csrfTokenSignal.asReadonly();
  readonly currentUser = this.userSignal.asReadonly();
  readonly initialized = this.initializedSignal.asReadonly();
  readonly sessionExpiredMessage = this.sessionExpiredMessageSignal.asReadonly();

  readonly isAuthenticated = computed(
    () => this.accessTokenSignal() !== null && this.userSignal() !== null
  );
  readonly userScope = computed<UserScope | null>(() => this.userSignal()?.userScope ?? null);
  readonly hospitalId = computed<number | null>(() => this.userSignal()?.hospitalId ?? null);
  readonly roles = computed<readonly string[]>(() => this.userSignal()?.roles ?? []);
  readonly permissions = computed<readonly string[]>(() => this.userSignal()?.permissions ?? []);

  setSession(accessToken: string, csrfToken: string, user: AuthUser): void {
    this.accessTokenSignal.set(accessToken);
    this.csrfTokenSignal.set(csrfToken);
    this.userSignal.set(user);
    this.sessionExpiredMessageSignal.set(null);
    this.initializedSignal.set(true);
  }

  setCsrfToken(csrfToken: string | null): void {
    this.csrfTokenSignal.set(csrfToken);
  }

  updateUser(user: AuthUser): void {
    this.userSignal.set(user);
  }

  markInitialized(): void {
    this.initializedSignal.set(true);
  }

  clearSession(expiredMessage?: string): void {
    this.accessTokenSignal.set(null);
    this.userSignal.set(null);
    this.sessionExpiredMessageSignal.set(expiredMessage ?? null);
  }

  clearSessionExpiredMessage(): void {
    this.sessionExpiredMessageSignal.set(null);
  }

  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }

  hasAnyPermission(requiredPermissions?: readonly string[]): boolean {
    if (!requiredPermissions || requiredPermissions.length === 0) {
      return true;
    }
    const granted = this.permissions();
    return requiredPermissions.some((permission) => granted.includes(permission));
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  canAccess(requiredPermissions?: readonly string[], requiredScope?: UserScope): boolean {
    if (!this.isAuthenticated()) {
      return false;
    }
    if (requiredScope && this.userScope() !== requiredScope) {
      return false;
    }
    return this.hasAnyPermission(requiredPermissions);
  }

  isPlatformUser(): boolean {
    return this.userScope() === 'PLATFORM';
  }

  isHospitalUser(): boolean {
    return this.userScope() === 'HOSPITAL';
  }
}
