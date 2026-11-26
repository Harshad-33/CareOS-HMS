import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { LeaveApplication, LeaveOverview, LeaveType } from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';
import { NotificationService } from '../../../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../../../shared/components/page-header/page-header';
import { LoadingIndicatorComponent } from '../../../../../shared/components/loading-indicator/loading-indicator';
import { EmptyStateComponent } from '../../../../../shared/components/empty-state/empty-state';
import { LeaveDetailDialogComponent } from '../leave-detail-dialog/leave-detail-dialog.component';

@Component({
  selector: 'app-leave-dashboard',
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
        title="Leave Management Dashboard"
        subtitle="Overview of hospital leave requests, active approvals, clinical staffing coverage, and policy management"
      >
        <button mat-flat-button color="primary" (click)="router.navigate(['/hr/leaves/inbox'])">
          <mat-icon>inbox</mat-icon> Approval Inbox
        </button>
      </app-page-header>

      <app-loading-indicator *ngIf="isLoading()" message="Loading leave overview metrics..."></app-loading-indicator>

      <ng-container *ngIf="!isLoading() && overview()">
        <!-- Metric Cards -->
        <div class="metrics-grid">
          <mat-card class="metric-card border-blue" (click)="router.navigate(['/hr/leaves/inbox'])">
            <div class="card-icon-wrapper bg-blue-light">
              <mat-icon class="icon-blue">pending_actions</mat-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ overview()?.pendingApprovals ?? 0 }}</div>
              <div class="metric-label">Pending Approvals</div>
            </div>
          </mat-card>

          <mat-card class="metric-card border-green" (click)="router.navigate(['/hr/leaves/calendar'])">
            <div class="card-icon-wrapper bg-green-light">
              <mat-icon class="icon-green">person_off</mat-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ overview()?.employeesOnLeaveToday ?? 0 }}</div>
              <div class="metric-label">On Leave Today</div>
            </div>
          </mat-card>

          <mat-card class="metric-card border-teal">
            <div class="card-icon-wrapper bg-teal-light">
              <mat-icon class="icon-teal">check_circle</mat-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ overview()?.approvedThisMonth ?? 0 }}</div>
              <div class="metric-label">Approved This Month</div>
            </div>
          </mat-card>

          <mat-card class="metric-card border-purple" (click)="router.navigate(['/hr/leaves/types'])">
            <div class="card-icon-wrapper bg-purple-light">
              <mat-icon class="icon-purple">policy</mat-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ overview()?.totalLeaveTypes ?? 0 }}</div>
              <div class="metric-label">Active Leave Policies</div>
            </div>
          </mat-card>
        </div>

        <!-- Quick Navigation Bar -->
        <div class="nav-cards-grid">
          <div class="nav-tile" (click)="router.navigate(['/hr/leaves/inbox'])">
            <mat-icon class="tile-icon text-amber">inbox</mat-icon>
            <div class="tile-title">Approval Inbox</div>
            <div class="tile-sub">Review employee requests</div>
          </div>

          <div class="nav-tile" (click)="router.navigate(['/hr/leaves/calendar'])">
            <mat-icon class="tile-icon text-indigo">calendar_month</mat-icon>
            <div class="tile-title">Leave Calendar</div>
            <div class="tile-sub">Staffing coverage view</div>
          </div>

          <div class="nav-tile" (click)="router.navigate(['/hr/leaves/balances'])">
            <mat-icon class="tile-icon text-emerald">account_balance_wallet</mat-icon>
            <div class="tile-title">Balance Management</div>
            <div class="tile-sub">Allocations & Ledger</div>
          </div>

          <div class="nav-tile" (click)="router.navigate(['/hr/leaves/types'])">
            <mat-icon class="tile-icon text-purple">settings_suggest</mat-icon>
            <div class="tile-title">Leave Policies</div>
            <div class="tile-sub">Entitlements & Workflows</div>
          </div>
        </div>

        <!-- Recent Applications Table -->
        <mat-card class="table-card">
          <mat-card-header class="table-header">
            <mat-card-title class="header-title">
              <mat-icon>history</mat-icon> Recent Leave Applications
            </mat-card-title>
          </mat-card-header>

          <mat-card-content class="p-0">
            <table mat-table [dataSource]="overview()?.recentApplications || []" class="w-full">
              <!-- Application Number -->
              <ng-container matColumnDef="applicationNumber">
                <th mat-header-cell *matHeaderCellDef>Application #</th>
                <td mat-cell *matCellDef="let app">
                  <span class="font-mono text-dark">{{ app.applicationNumber }}</span>
                  <span class="text-xs text-muted block">{{ app.appliedAt | date:'mediumDate' }}</span>
                </td>
              </ng-container>

              <!-- Employee -->
              <ng-container matColumnDef="employee">
                <th mat-header-cell *matHeaderCellDef>Employee</th>
                <td mat-cell *matCellDef="let app">
                  <div class="font-medium text-dark">{{ app.employeeName }}</div>
                  <div class="text-muted text-xs">{{ app.employeeNumber }} • {{ app.departmentName || 'No Department' }}</div>
                </td>
              </ng-container>

              <!-- Leave Type -->
              <ng-container matColumnDef="leaveType">
                <th mat-header-cell *matHeaderCellDef>Leave Type</th>
                <td mat-cell *matCellDef="let app">
                  <span class="type-pill">{{ app.leaveTypeName }}</span>
                </td>
              </ng-container>

              <!-- Duration -->
              <ng-container matColumnDef="duration">
                <th mat-header-cell *matHeaderCellDef>Dates & Duration</th>
                <td mat-cell *matCellDef="let app">
                  <div class="text-dark">{{ app.startDate }} to {{ app.endDate }}</div>
                  <div class="text-primary text-xs font-semibold">
                    {{ app.totalDays }} Day(s)
                    <span *ngIf="app.halfDay">({{ app.halfDayPeriod }})</span>
                  </div>
                </td>
              </ng-container>

              <!-- Status -->
              <ng-container matColumnDef="status">
                <th mat-header-cell *matHeaderCellDef>Status</th>
                <td mat-cell *matCellDef="let app">
                  <span [class]="'status-pill pill-' + app.status.toLowerCase()">
                    {{ app.status }}
                  </span>
                  <span *ngIf="app.status === 'PENDING'" class="text-xs text-muted block mt-1">
                    Step {{ app.currentStepNumber }} of {{ app.requiredStepsCount }}
                  </span>
                </td>
              </ng-container>

              <!-- Actions -->
              <ng-container matColumnDef="actions">
                <th mat-header-cell *matHeaderCellDef class="text-right">Action</th>
                <td mat-cell *matCellDef="let app" class="text-right">
                  <button mat-icon-button matTooltip="View Details" (click)="openDetailDialog(app)">
                    <mat-icon color="primary">visibility</mat-icon>
                  </button>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
            </table>

            <app-empty-state
              *ngIf="(overview()?.recentApplications?.length || 0) === 0"
              icon="event_note"
              title="No Recent Applications"
              description="No leave applications have been submitted recently."
            ></app-empty-state>
          </mat-card-content>
        </mat-card>
      </ng-container>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin: 16px 0 20px 0;
    }
    .metric-card {
      display: flex;
      flex-direction: row;
      align-items: center;
      gap: 16px;
      padding: 16px 20px;
      cursor: pointer;
      border: 1px solid #e2e8f0;
      transition: all 0.2s ease;
    }
    .metric-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
      transform: translateY(-2px);
    }
    .card-icon-wrapper {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .bg-blue-light { background: #eff6ff; }
    .bg-green-light { background: #f0fdf4; }
    .bg-teal-light { background: #f0fdfa; }
    .bg-purple-light { background: #faf5ff; }
    .icon-blue { color: #2563eb; font-size: 26px; }
    .icon-green { color: #16a34a; font-size: 26px; }
    .icon-teal { color: #0d9488; font-size: 26px; }
    .icon-purple { color: #9333ea; font-size: 26px; }
    .metric-value { font-size: 1.8rem; font-weight: 700; color: #1e293b; line-height: 1.1; }
    .metric-label { font-size: 0.8rem; color: #64748b; margin-top: 4px; }
    .border-blue { border-left: 4px solid #2563eb; }
    .border-green { border-left: 4px solid #16a34a; }
    .border-teal { border-left: 4px solid #0d9488; }
    .border-purple { border-left: 4px solid #9333ea; }

    .nav-cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
      margin-bottom: 24px;
    }
    .nav-tile {
      background: white;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 14px 16px;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .nav-tile:hover {
      background: #f8fafc;
      border-color: #cbd5e1;
      transform: translateY(-2px);
    }
    .tile-icon { font-size: 24px; width: 24px; height: 24px; margin-bottom: 6px; }
    .tile-title { font-weight: 600; font-size: 0.95rem; color: #1e293b; }
    .tile-sub { font-size: 0.75rem; color: #64748b; margin-top: 2px; }
    .text-amber { color: #d97706; }
    .text-indigo { color: #4f46e5; }
    .text-emerald { color: #059669; }
    .text-purple { color: #7c3aed; }

    .table-card { overflow: hidden; }
    .table-header { padding: 16px 20px 8px 20px; }
    .header-title { display: flex; align-items: center; gap: 8px; font-size: 1.1rem; }
    .w-full { width: 100%; }
    .p-0 { padding: 0 !important; }
    .font-mono { font-family: monospace; font-weight: 600; }
    .text-dark { color: #1e293b; }
    .text-muted { color: #64748b; }
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
  `],
})
export class LeaveDashboardComponent implements OnInit {
  readonly router = inject(Router);
  private readonly leaveService = inject(LeaveService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly isLoading = signal(true);
  readonly overview = signal<LeaveOverview | null>(null);

  readonly displayedColumns: string[] = ['applicationNumber', 'employee', 'leaveType', 'duration', 'status', 'actions'];

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    this.isLoading.set(true);
    this.leaveService.getLeaveOverview().subscribe({
      next: (data) => {
        this.overview.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.notificationService.error('Failed to load leave overview');
        this.isLoading.set(false);
      },
    });
  }

  openDetailDialog(app: LeaveApplication): void {
    this.dialog.open(LeaveDetailDialogComponent, {
      width: '650px',
      data: app,
    });
  }
}
