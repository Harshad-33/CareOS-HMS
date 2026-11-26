import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { CreateLeaveTypeRequest, LeaveType, UpdateLeaveTypeRequest } from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';
import { NotificationService } from '../../../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../../../shared/components/page-header/page-header';
import { LoadingIndicatorComponent } from '../../../../../shared/components/loading-indicator/loading-indicator';
import { EmptyStateComponent } from '../../../../../shared/components/empty-state/empty-state';
import { ConfirmDialogComponent } from '../../../../../shared/components/confirm-dialog/confirm-dialog';
import { ConfirmDialogData } from '../../../../../shared/models/confirm-dialog.model';
import { LeaveTypeDialogComponent } from '../leave-type-dialog/leave-type-dialog.component';

@Component({
  selector: 'app-leave-type-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatMenuModule,
    PageHeaderComponent,
    LoadingIndicatorComponent,
    EmptyStateComponent,
  ],
  template: `
    <div class="page-container">
      <app-page-header
        title="Leave Policies & Entitlements"
        subtitle="Manage configurable hospital leave types, annual allowances and approval workflows"
      >
        <button mat-flat-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon>
          New Leave Policy
        </button>
      </app-page-header>

      <app-loading-indicator *ngIf="isLoading()" message="Loading leave policies..."></app-loading-indicator>

      <mat-card *ngIf="!isLoading()" class="table-card">
        <mat-card-content class="p-0">
          <table mat-table [dataSource]="leaveTypes()" class="w-full">
            <!-- Code Column -->
            <ng-container matColumnDef="code">
              <th mat-header-cell *matHeaderCellDef>Code</th>
              <td mat-cell *matCellDef="let lt">
                <span class="code-badge">{{ lt.code }}</span>
              </td>
            </ng-container>

            <!-- Name Column -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Policy Name</th>
              <td mat-cell *matCellDef="let lt">
                <div class="font-medium text-dark">{{ lt.name }}</div>
                <div *ngIf="lt.description" class="text-muted text-sm">{{ lt.description }}</div>
              </td>
            </ng-container>

            <!-- Allowance Column -->
            <ng-container matColumnDef="allowance">
              <th mat-header-cell *matHeaderCellDef>Annual Entitlement</th>
              <td mat-cell *matCellDef="let lt">
                <strong>{{ lt.annualAllowance }}</strong> days / year
              </td>
            </ng-container>

            <!-- Carry Forward Column -->
            <ng-container matColumnDef="carryForward">
              <th mat-header-cell *matHeaderCellDef>Carry Forward</th>
              <td mat-cell *matCellDef="let lt">
                <span *ngIf="lt.carryForwardAllowed" class="text-success">
                  Yes (Max {{ lt.maxCarryForwardDays }} days)
                </span>
                <span *ngIf="!lt.carryForwardAllowed" class="text-muted">No</span>
              </td>
            </ng-container>

            <!-- Rules Column -->
            <ng-container matColumnDef="rules">
              <th mat-header-cell *matHeaderCellDef>Rules & Requirements</th>
              <td mat-cell *matCellDef="let lt">
                <div class="rules-tags">
                  <span *ngIf="lt.requiresDocument" class="rule-badge badge-warn">Medical Cert Required</span>
                  <span *ngIf="lt.allowHalfDay" class="rule-badge badge-info">Half-Day Allowed</span>
                  <span *ngIf="lt.maxConsecutiveDays > 0" class="rule-badge badge-neutral">Max {{ lt.maxConsecutiveDays }} consecutive</span>
                </div>
              </td>
            </ng-container>

            <!-- Workflow Column -->
            <ng-container matColumnDef="workflow">
              <th mat-header-cell *matHeaderCellDef>Approval Workflow</th>
              <td mat-cell *matCellDef="let lt">
                <span class="workflow-chip">{{ formatWorkflow(lt.approvalWorkflow) }}</span>
                <span class="text-muted text-xs block">({{ lt.approvalLevelsCount }} Level{{ lt.approvalLevelsCount > 1 ? 's' : '' }})</span>
              </td>
            </ng-container>

            <!-- Status Column -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let lt">
                <span [class]="'status-pill ' + (lt.status === 'ACTIVE' ? 'pill-active' : 'pill-inactive')">
                  {{ lt.status }}
                </span>
              </td>
            </ng-container>

            <!-- Actions Column -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef class="text-right">Actions</th>
              <td mat-cell *matCellDef="let lt" class="text-right">
                <button mat-icon-button [matMenuTriggerFor]="menu" aria-label="Policy actions">
                  <mat-icon>more_vert</mat-icon>
                </button>
                <mat-menu #menu="matMenu">
                  <button mat-menu-item (click)="openEditDialog(lt)">
                    <mat-icon>edit</mat-icon>
                    <span>Edit Policy</span>
                  </button>
                  <button *ngIf="lt.status === 'ACTIVE'" mat-menu-item (click)="toggleStatus(lt, false)">
                    <mat-icon color="warn">pause_circle</mat-icon>
                    <span>Deactivate</span>
                  </button>
                  <button *ngIf="lt.status === 'INACTIVE'" mat-menu-item (click)="toggleStatus(lt, true)">
                    <mat-icon color="primary">play_circle</mat-icon>
                    <span>Activate</span>
                  </button>
                </mat-menu>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>

          <app-empty-state
            *ngIf="leaveTypes().length === 0"
            icon="event_note"
            title="No Leave Policies"
            description="No leave policies have been configured yet. Create policies such as Casual, Sick, or Earned leave."
          ></app-empty-state>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container {
      padding: 24px;
    }
    .table-card {
      margin-top: 16px;
      overflow: hidden;
    }
    .w-full { width: 100%; }
    .p-0 { padding: 0 !important; }
    .code-badge {
      font-family: monospace;
      font-weight: 700;
      background: #eef2f6;
      padding: 3px 8px;
      border-radius: 4px;
      color: #1e3a8a;
    }
    .text-dark { color: #111827; }
    .text-muted { color: #6b7280; }
    .text-sm { font-size: 0.85rem; }
    .text-xs { font-size: 0.75rem; }
    .text-success { color: #15803d; font-weight: 500; }
    .text-right { text-align: right; }
    .block { display: block; }
    .rules-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }
    .rule-badge {
      font-size: 0.75rem;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 500;
    }
    .badge-warn { background: #fef3c7; color: #b45309; }
    .badge-info { background: #e0f2fe; color: #0369a1; }
    .badge-neutral { background: #f3f4f6; color: #4b5563; }
    .workflow-chip {
      font-size: 0.8rem;
      font-weight: 600;
      color: #374151;
    }
    .status-pill {
      font-size: 0.75rem;
      padding: 3px 10px;
      border-radius: 12px;
      font-weight: 600;
    }
    .pill-active { background: #dcfce7; color: #166534; }
    .pill-inactive { background: #f3f4f6; color: #6b7280; }
  `],
})
export class LeaveTypeListComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly isLoading = signal(true);
  readonly leaveTypes = signal<LeaveType[]>([]);
  readonly displayedColumns: string[] = [
    'code',
    'name',
    'allowance',
    'carryForward',
    'rules',
    'workflow',
    'status',
    'actions',
  ];

  ngOnInit(): void {
    this.loadLeaveTypes();
  }

  loadLeaveTypes(): void {
    this.isLoading.set(true);
    this.leaveService.getLeaveTypes().subscribe({
      next: (types) => {
        this.leaveTypes.set(types);
        this.isLoading.set(false);
      },
      error: () => {
        this.notificationService.error('Failed to load leave policies');
        this.isLoading.set(false);
      },
    });
  }

  formatWorkflow(wf: string): string {
    switch (wf) {
      case 'HR_ONLY':
        return 'HR Admin Only';
      case 'MANAGER_ONLY':
        return 'Reporting Manager Only';
      case 'MANAGER_THEN_HR':
        return 'Manager → HR Admin';
      case 'DEPT_HEAD_THEN_HR':
        return 'Department Head → HR Admin';
      default:
        return wf;
    }
  }

  openCreateDialog(): void {
    const ref = this.dialog.open(LeaveTypeDialogComponent, {
      width: '560px',
      data: {},
    });

    ref.afterClosed().subscribe((res: CreateLeaveTypeRequest | undefined) => {
      if (!res) return;
      this.leaveService.createLeaveType(res).subscribe({
        next: () => {
          this.notificationService.success('Leave policy created successfully');
          this.loadLeaveTypes();
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to create leave policy');
        },
      });
    });
  }

  openEditDialog(type: LeaveType): void {
    const ref = this.dialog.open(LeaveTypeDialogComponent, {
      width: '560px',
      data: { leaveType: type },
    });

    ref.afterClosed().subscribe((res: UpdateLeaveTypeRequest | undefined) => {
      if (!res) return;
      this.leaveService.updateLeaveType(type.publicId, res).subscribe({
        next: () => {
          this.notificationService.success('Leave policy updated successfully');
          this.loadLeaveTypes();
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to update leave policy');
        },
      });
    });
  }

  toggleStatus(type: LeaveType, activate: boolean): void {
    const action = activate ? 'activate' : 'deactivate';
    const dialogData: ConfirmDialogData = {
      title: `${activate ? 'Activate' : 'Deactivate'} Policy`,
      message: `Are you sure you want to ${action} the "${type.name}" policy?`,
      confirmLabel: activate ? 'Activate' : 'Deactivate',
      severity: activate ? 'primary' : 'warn',
    };

    const ref = this.dialog.open(ConfirmDialogComponent, { data: dialogData });
    ref.afterClosed().subscribe((confirmed) => {
      if (!confirmed) return;

      const call = activate
        ? this.leaveService.activateLeaveType(type.publicId)
        : this.leaveService.deactivateLeaveType(type.publicId);

      call.subscribe({
        next: () => {
          this.notificationService.success(`Policy ${action}d successfully`);
          this.loadLeaveTypes();
        },
        error: () => {
          this.notificationService.error(`Failed to ${action} policy`);
        },
      });
    });
  }
}
