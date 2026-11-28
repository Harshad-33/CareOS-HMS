import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { NotificationService } from '../../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyStateComponent } from '../../../../../shared/components/empty-state/empty-state';
import { LoadingIndicatorComponent } from '../../../../../shared/components/loading-indicator/loading-indicator';
import { PageHeaderComponent } from '../../../../../shared/components/page-header/page-header';
import { ConfirmDialogData } from '../../../../../shared/models/confirm-dialog.model';
import {
  ApplyLeaveRequest,
  LeaveApplicationSummary,
  LeaveBalance,
  LeaveTypeSummary,
} from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';
import { ApplyLeaveDialogComponent } from '../apply-leave-dialog/apply-leave-dialog.component';
import { LeaveDetailDialogComponent } from '../leave-detail-dialog/leave-detail-dialog.component';

@Component({
  selector: 'app-my-leaves',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatChipsModule,
    PageHeaderComponent,
    LoadingIndicatorComponent,
    EmptyStateComponent,
  ],
  template: `
    <div class="page-container">
      <app-page-header
        title="My Leave Portal"
        subtitle="View your personal leave balances, submit leave applications, and track approval status"
      >
        <button mat-flat-button color="primary" (click)="openApplyDialog()">
          <mat-icon>add</mat-icon> Apply for Leave
        </button>
      </app-page-header>

      <app-loading-indicator *ngIf="isLoading()" message="Loading personal leave details..."></app-loading-indicator>

      <ng-container *ngIf="!isLoading()">
        <!-- Personal Balances Cards -->
        <div class="balances-section">
          <h3 class="section-heading">
            <mat-icon>account_balance_wallet</mat-icon>
            Available Leave Balances ({{ currentYear }})
          </h3>

          <div class="balances-grid">
            <mat-card *ngFor="let bal of balances()" class="balance-card">
              <mat-card-header class="bal-header">
                <mat-card-title class="bal-title">{{ bal.leaveTypeName }}</mat-card-title>
                <span class="bal-code">{{ bal.leaveTypeCode }}</span>
              </mat-card-header>
              <mat-card-content class="bal-content">
                <div class="bal-number">{{ bal.availableBalance }}</div>
                <div class="bal-label">Days Available</div>
                <div class="bal-breakdown">
                  <span class="breakdown-item">Allocated: {{ bal.totalAllocated }}</span>
                  <span class="breakdown-item text-danger">Used: {{ bal.usedDays }}</span>
                  <span class="breakdown-item text-warn">Pending: {{ bal.pendingApprovalDays }}</span>
                </div>
              </mat-card-content>
            </mat-card>
          </div>

          <app-empty-state
            *ngIf="balances().length === 0"
            icon="account_balance_wallet"
            title="No Balances Allocated"
            description="No leave balances have been allocated to your account for this year. Please contact HR."
          ></app-empty-state>
        </div>

        <!-- My Applications Table -->
        <div class="applications-section">
          <h3 class="section-heading">
            <mat-icon>history</mat-icon>
            My Application History
          </h3>

          <mat-card class="table-card">
            <mat-card-content class="p-0">
              <table mat-table [dataSource]="applications()" class="w-full">
                <!-- App ID Column -->
                <ng-container matColumnDef="appId">
                  <th mat-header-cell *matHeaderCellDef>Reference</th>
                  <td mat-cell *matCellDef="let app">
                    <span class="font-mono text-dark">{{ app.publicId | slice:0:8 }}</span>
                    <span class="text-xs text-muted block">{{ app.appliedAt | date:'mediumDate' }}</span>
                  </td>
                </ng-container>

                <!-- Leave Type Column -->
                <ng-container matColumnDef="leaveType">
                  <th mat-header-cell *matHeaderCellDef>Leave Type</th>
                  <td mat-cell *matCellDef="let app">
                    <span class="type-pill">{{ app.leaveTypeName }}</span>
                  </td>
                </ng-container>

                <!-- Dates Column -->
                <ng-container matColumnDef="dates">
                  <th mat-header-cell *matHeaderCellDef>Dates</th>
                  <td mat-cell *matCellDef="let app">
                    <div class="text-dark">{{ app.startDate }} to {{ app.endDate }}</div>
                    <div class="text-primary text-xs font-semibold">
                      {{ app.totalDays }} Day(s)
                      <span *ngIf="app.halfDay">(Half Day)</span>
                    </div>
                  </td>
                </ng-container>

                <!-- Progress / Status Column -->
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>Status & Progress</th>
                  <td mat-cell *matCellDef="let app">
                    <span [class]="'status-pill pill-' + app.status.toLowerCase()">
                      {{ app.status }}
                    </span>
                    <span *ngIf="app.status === 'PENDING'" class="text-xs text-muted block mt-1">
                      Step {{ app.currentStepNumber }} of {{ app.requiredStepsCount }}
                    </span>
                  </td>
                </ng-container>

                <!-- Actions Column -->
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef class="text-right">Actions</th>
                  <td mat-cell *matCellDef="let app" class="text-right">
                    <div class="actions-group">
                      <button mat-icon-button matTooltip="View Details & Timeline" (click)="openDetailDialog(app)">
                        <mat-icon color="primary">visibility</mat-icon>
                      </button>
                      <button
                        *ngIf="canCancel(app)"
                        mat-stroked-button
                        color="warn"
                        class="btn-sm"
                        (click)="cancelApplication(app)"
                      >
                        <mat-icon>cancel</mat-icon> Cancel
                      </button>
                    </div>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>

              <app-empty-state
                *ngIf="applications().length === 0"
                icon="event_note"
                title="No Leave Applications"
                description="You have not submitted any leave applications yet. Click 'Apply for Leave' to submit your first request."
              ></app-empty-state>
            </mat-card-content>
          </mat-card>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .section-heading {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1.1rem;
      font-weight: 600;
      color: #1e293b;
      margin: 20px 0 12px 0;
    }
    .balances-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .balance-card { border: 1px solid #e2e8f0; overflow: hidden; }
    .bal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #f8fafc;
      padding: 10px 14px;
      border-bottom: 1px solid #e2e8f0;
    }
    .bal-title { font-size: 0.95rem; font-weight: 600; color: #1e293b; }
    .bal-code { font-size: 0.75rem; background: #e2e8f0; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
    .bal-content {
      padding: 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .bal-number { font-size: 2.2rem; font-weight: 700; color: #1e40af; line-height: 1; }
    .bal-label { font-size: 0.75rem; text-transform: uppercase; color: #64748b; margin-top: 4px; margin-bottom: 12px; }
    .bal-breakdown {
      display: flex;
      justify-content: space-between;
      width: 100%;
      font-size: 0.75rem;
      color: #64748b;
      background: #f8fafc;
      padding: 6px 10px;
      border-radius: 4px;
    }
    .table-card { overflow: hidden; }
    .w-full { width: 100%; }
    .p-0 { padding: 0 !important; }
    .font-mono { font-family: monospace; font-weight: 600; }
    .text-dark { color: #1e293b; }
    .text-muted { color: #64748b; }
    .text-danger { color: #dc2626; }
    .text-warn { color: #d97706; }
    .text-primary { color: #1d4ed8; }
    .text-xs { font-size: 0.75rem; }
    .block { display: block; }
    .mt-1 { margin-top: 2px; }
    .text-right { text-align: right; }
    .type-pill {
      background: #eff6ff;
      color: #1e40af;
      padding: 3px 8px;
      border-radius: 4px;
      font-weight: 600;
      font-size: 0.8rem;
    }
    .status-pill {
      font-size: 0.75rem;
      padding: 3px 10px;
      border-radius: 12px;
      font-weight: 600;
    }
    .pill-pending { background: #fef3c7; color: #92400e; }
    .pill-approved { background: #dcfce7; color: #166534; }
    .pill-rejected { background: #fee2e2; color: #991b1b; }
    .pill-cancelled { background: #f3f4f6; color: #4b5563; }
    .actions-group { display: inline-flex; align-items: center; gap: 6px; }
    .btn-sm { padding: 0 10px; height: 30px; font-size: 0.8rem; }
  `],
})
export class MyLeavesComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly isLoading = signal(true);
  readonly balances = signal<LeaveBalance[]>([]);
  readonly applications = signal<LeaveApplicationSummary[]>([]);
  readonly leaveTypes = signal<LeaveTypeSummary[]>([]);

  readonly currentYear = new Date().getFullYear();
  readonly displayedColumns: string[] = ['appId', 'leaveType', 'dates', 'status', 'actions'];

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    this.leaveService.getMyBalances(this.currentYear).subscribe({
      next: (bals) => this.balances.set(bals),
    });

    this.leaveService.getMyActiveLeaveTypes().subscribe({
      next: (types) => this.leaveTypes.set(types),
    });

    this.leaveService.getMyApplications().subscribe({
      next: (page) => {
        this.applications.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.notificationService.error('Failed to load leave records');
        this.isLoading.set(false);
      },
    });
  }

  canCancel(app: LeaveApplicationSummary): boolean {
    if (app.status === 'PENDING') return true;
    if (app.status === 'APPROVED') {
      const today = new Date().toISOString().split('T')[0];
      return app.startDate > today;
    }
    return false;
  }

  openApplyDialog(): void {
    const ref = this.dialog.open(ApplyLeaveDialogComponent, {
      width: '560px',
      data: {
        leaveTypes: this.leaveTypes(),
        balances: this.balances(),
      },
    });

    ref.afterClosed().subscribe((req: ApplyLeaveRequest | undefined) => {
      if (!req) return;
      this.leaveService.applyLeave(req).subscribe({
        next: (created) => {
          this.notificationService.success(
            `Leave application (${created.publicId.slice(0, 8)}) submitted successfully`
          );
          this.loadData();
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to submit leave application');
        },
      });
    });
  }

  cancelApplication(app: LeaveApplicationSummary): void {
    const data: ConfirmDialogData = {
      title: 'Cancel Leave Application',
      message: `Are you sure you want to cancel your ${app.leaveTypeName} application (${app.startDate} to ${app.endDate})?`,
      confirmLabel: 'Yes, Cancel',
      severity: 'warn',
    };

    const ref = this.dialog.open(ConfirmDialogComponent, { data });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;
      this.leaveService.cancelMyApplication(app.publicId).subscribe({
        next: () => {
          this.notificationService.success('Leave application cancelled successfully');
          this.loadData();
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to cancel leave application');
        },
      });
    });
  }

  openDetailDialog(app: LeaveApplicationSummary): void {
    this.dialog.open(LeaveDetailDialogComponent, {
      width: '650px',
      data: {
        applicationPublicId: app.publicId,
        selfService: true,
      },
    });
  }
}
