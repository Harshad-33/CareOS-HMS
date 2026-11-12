import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { NormalizedHttpError } from '../../core/interceptors/http-foundation.interceptor';
import { HealthStatus } from '../../core/models/health-status.model';
import { AuthStateService } from '../../core/services/auth-state.service';
import { HealthService } from '../../core/services/health.service';
import { NotificationService } from '../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header';
import { ConfirmDialogData } from '../../shared/models/confirm-dialog.model';
import { SafeEmptyPipe } from '../../shared/pipes/safe-empty.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    LoadingIndicatorComponent,
    ErrorStateComponent,
    SafeEmptyPipe,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent implements OnInit {
  private readonly healthService = inject(HealthService);
  private readonly authState = inject(AuthStateService);
  private readonly dialog = inject(MatDialog);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(true);
  readonly health = signal<HealthStatus | null>(null);
  readonly error = signal<NormalizedHttpError | null>(null);
  readonly currentUser = this.authState.currentUser;

  ngOnInit(): void {
    this.loadSystemHealth();
  }

  loadSystemHealth(): void {
    this.loading.set(true);
    this.error.set(null);

    this.healthService.getHealth().subscribe({
      next: (status) => {
        this.health.set(status);
        this.loading.set(false);
      },
      error: (err: NormalizedHttpError) => {
        this.health.set(null);
        this.error.set(err);
        this.loading.set(false);
      },
    });
  }

  confirmRefresh(): void {
    const dialogData: ConfirmDialogData = {
      title: 'Run Health Check',
      message: 'Re-verify backend API and MySQL database connectivity now?',
      confirmLabel: 'Check Now',
      cancelLabel: 'Cancel',
      severity: 'primary',
    };

    this.dialog
      .open(ConfirmDialogComponent, { data: dialogData, width: '420px' })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (confirmed) {
          this.loadSystemHealth();
          this.notificationService.info('Refreshing system health status...');
        }
      });
  }
}
