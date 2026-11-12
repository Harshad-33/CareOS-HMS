import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { APP_CONSTANTS } from '../../../core/constants/app.constants';
import { NormalizedHttpError } from '../../../core/interceptors/http-foundation.interceptor';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TrimInputDirective } from '../../../shared/directives/trim-input.directive';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    TrimInputDirective,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly authState = inject(AuthStateService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly appName = APP_CONSTANTS.applicationName;
  readonly appSubtitle = APP_CONSTANTS.applicationSubtitle;
  readonly hidePassword = signal(true);
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly sessionExpiredNotice = this.authState.sessionExpiredMessage;

  readonly loginForm = this.fb.nonNullable.group({
    usernameOrEmail: ['', [Validators.required, Validators.maxLength(160)]],
    password: ['', [Validators.required, Validators.maxLength(128)]],
    rememberMe: [false],
  });

  togglePasswordVisibility(): void {
    this.hidePassword.update((hidden) => !hidden);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.authState.clearSessionExpiredMessage();

    const { usernameOrEmail, password, rememberMe } = this.loginForm.getRawValue();

    this.authService
      .login({
        usernameOrEmail: usernameOrEmail.trim(),
        password,
        rememberMe,
      })
      .subscribe({
        next: (response) => {
          this.submitting.set(false);
          this.notificationService.info(`Signed in as ${response.user.fullName}`);
          const returnUrl = this.resolveSafeReturnUrl();
          void this.router.navigateByUrl(returnUrl);
        },
        error: (err: NormalizedHttpError) => {
          this.submitting.set(false);
          this.errorMessage.set(
            err.message || 'Unable to sign in. Please verify your credentials and try again.'
          );
        },
      });
  }

  private resolveSafeReturnUrl(): string {
    const candidate = this.route.snapshot.queryParamMap.get('returnUrl');
    if (candidate && candidate.startsWith('/') && !candidate.startsWith('//') && !candidate.startsWith('/login')) {
      return candidate;
    }
    return '/dashboard';
  }
}
