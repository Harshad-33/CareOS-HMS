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
import { EmptyStateComponent } from '../../../../../shared/components/empty-state/empty-state';
import { LoadingIndicatorComponent } from '../../../../../shared/components/loading-indicator/loading-indicator';
import { PageHeaderComponent } from '../../../../../shared/components/page-header/page-header';
import { LeaveApprovalInboxItem, RejectLeaveStepRequest } from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';
import { LeaveDetailDialogComponent } from '../leave-detail-dialog/leave-detail-dialog.component';
import { RejectReasonDialogComponent } from '../reject-reason-dialog/reject-reason-dialog.component';

@Component({
  selector: 'app-approval-inbox',
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
        title="Leave Approval Inbox"
        subtitle="Review and action pending leave applications assigned to you for approval"
      >
        <button mat-stroked-button (click)="loadPendingApprovals()">
          <mat-icon>refresh</mat-icon>
          Refresh
        </button>
      </app-page-header>

      <app-loading-indicator *ngIf="isLoading()" message="Checking pending approvals..."></app-loading-indicator>

      <mat-card *ngIf="!isLoading()" class="table-card">
        <mat-card-content class="p-0">
          <table mat-table [dataSource]="items()" class="w-full">
            <!-- Reference & Date -->
            <ng-container matColumnDef="applicationId">
              <th mat-header-cell *matHeaderCellDef>Reference</th>
              <td mat-cell *matCellDef="let item">
                <span class="font-mono text-dark">{{ item.applicationPublicId | slice:0:8 }}</span>
                <span class="text-muted text-xs block">{{ item.appliedAt | date:'mediumDate' }}</span>
              </td>
            </ng-container>

            <!-- Employee Column -->
            <ng-container matColumnDef="employee">
              <th mat-header-cell *matHeaderCellDef>Employee</th>
              <td mat-cell *matCellDef="let item">
                <div class="font-medium text-dark">{{ item.employeeName }}</div>
                <div class="text-muted text-sm">{{ item.employeeNumber }} • {{ item.departmentName || 'No Department' }}</div>
              </td>
            </ng-container>

            <!-- Leave Type Column -->
            <ng-container matColumnDef="leaveType">
              <th mat-header-cell *matHeaderCellDef>Leave Type</th>
              <td mat-cell *matCellDef="let item">
                <span class="type-pill">{{ item.leaveTypeName }}</span>
              </td>
            </ng-container>

            <!-- Duration Column -->
            <ng-container matColumnDef="duration">
              <th mat-header-cell *matHeaderCellDef>Duration</th>
              <td mat-cell *matCellDef="let item">
                <div class="text-dark">{{ item.startDate }} to {{ item.endDate }}</div>
                <div class="text-primary text-xs font-semibold">
                  {{ item.totalDays }} Day(s)
                  <span *ngIf="item.halfDay">({{ item.halfDayPeriod }})</span>
                </div>
              </td>
            </ng-container>

            <!-- Step Column -->
            <ng-container matColumnDef="step">
              <th mat-header-cell *matHeaderCellDef>Active Step</th>
              <td mat-cell *matCellDef="let item">
                <span class="step-chip">Step {{ item.stepNumber }} of {{ item.requiredStepsCount }}</span>
                <span class="text-muted text-xs block mt-1">{{ formatApproverRole(item.approverRole) }}</span>
              </td>
            </ng-container>

            <!-- Reason Column -->
            <ng-container matColumnDef="reason">
              <th mat-header-cell *matHeaderCellDef>Reason</th>
              <td mat-cell *matCellDef="let item">
                <div class="reason-preview" [matTooltip]="item.reason">{{ item.reason }}</div>
              </td>
            </ng-container>

            <!-- Actions Column -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef class="text-right">Actions</th>
              <td mat-cell *matCellDef="let item" class="text-right">
                <div class="actions-group">
                  <button mat-icon-button matTooltip="View Details" (click)="openDetailDialog(item)">
                    <mat-icon color="primary">visibility</mat-icon>
                  </button>
                  <button mat-flat-button color="primary" class="btn-sm" (click)="approve(item)">
                    <mat-icon>check</mat-icon> Approve
                  </button>
                  <button mat-stroked-button color="warn" class="btn-sm" (click)="openRejectDialog(item)">
                    <mat-icon>close</mat-icon> Reject
                  </button>
                </div>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>

          <app-empty-state
            *ngIf="items().length === 0"
            icon="task_alt"
            title="All Caught Up!"
            description="No pending leave applications currently require your review or approval."
          ></app-empty-state>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .table-card { margin-top: 16px; overflow: hidden; }
    .w-full { width: 100%; }
    .p-0 { padding: 0 !important; }
    .font-mono { font-family: monospace; font-weight: 600; }
    .text-dark { color: #1e293b; }
    .text-muted { color: #64748b; }
    .text-primary { color: #1d4ed8; }
    .text-xs { font-size: 0.75rem; }
    .text-sm { font-size: 0.85rem; }
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
    .step-chip {
      background: #fef3c7;
      color: #92400e;
      padding: 3px 8px;
      border-radius: 12px;
      font-weight: 600;
      font-size: 0.75rem;
    }
    .reason-preview {
      max-width: 200px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 0.85rem;
      color: #475569;
    }
    .actions-group { display: inline-flex; align-items: center; gap: 6px; }
    .btn-sm { padding: 0 10px; height: 32px; font-size: 0.8rem; }
  `],
})
export class ApprovalInboxComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly isLoading = signal(true);
  readonly items = signal<LeaveApprovalInboxItem[]>([]);
  readonly displayedColumns: string[] = [
    'applicationId',
    'employee',
    'leaveType',
    'duration',
    'step',
    'reason',
    'actions',
  ];

  ngOnInit(): void {
    this.loadPendingApprovals();
  }

  formatApproverRole(role: string): string {
    switch (role) {
      case 'REPORTING_MANAGER':
        return 'Reporting Manager';
      case 'DEPARTMENT_HEAD':
        return 'Department Head';
      case 'HR_MANAGER':
        return 'HR Manager';
      default:
        return role;
    }
  }

  loadPendingApprovals(): void {
    this.isLoading.set(true);
    this.leaveService.getPendingApprovals().subscribe({
      next: (page) => {
        this.items.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.notificationService.error('Failed to load pending approvals');
        this.isLoading.set(false);
      },
    });
  }

  approve(item: LeaveApprovalInboxItem): void {
    this.leaveService
      .approveStep(item.applicationPublicId, item.stepNumber, { comments: 'Approved via inbox' })
      .subscribe({
        next: () => {
          this.notificationService.success(
            `Leave step ${item.stepNumber} for ${item.employeeName} approved successfully`
          );
          this.loadPendingApprovals();
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to approve application');
        },
      });
  }

  openRejectDialog(item: LeaveApprovalInboxItem): void {
    const ref = this.dialog.open(RejectReasonDialogComponent, {
      width: '450px',
      data: {
        applicationLabel: `${item.employeeName} (${item.applicationPublicId.slice(0, 8)})`,
        stepNumber: item.stepNumber,
      },
    });

    ref.afterClosed().subscribe((res: RejectLeaveStepRequest | undefined) => {
      if (!res) return;
      this.leaveService.rejectStep(item.applicationPublicId, item.stepNumber, res).subscribe({
        next: () => {
          this.notificationService.warn(`Application for ${item.employeeName} rejected`);
          this.loadPendingApprovals();
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to reject application');
        },
      });
    });
  }

  openDetailDialog(item: LeaveApprovalInboxItem): void {
    this.dialog.open(LeaveDetailDialogComponent, {
      width: '650px',
      data: {
        applicationPublicId: item.applicationPublicId,
        selfService: false,
      },
    });
  }
}
