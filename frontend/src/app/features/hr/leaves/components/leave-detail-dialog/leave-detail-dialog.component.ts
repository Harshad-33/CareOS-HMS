import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { NotificationService } from '../../../../../core/services/notification.service';
import { LoadingIndicatorComponent } from '../../../../../shared/components/loading-indicator/loading-indicator';
import { LeaveApplication } from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';

export interface LeaveDetailDialogData {
  applicationPublicId: string;
  selfService?: boolean;
  initialApplication?: LeaveApplication;
}

@Component({
  selector: 'app-leave-detail-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    LoadingIndicatorComponent,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-title">
          <mat-icon class="header-icon">event_note</mat-icon>
          <div>
            <h2 mat-dialog-title class="title-text">Leave Application Details</h2>
            <span class="app-number">{{ application()?.publicId || data.applicationPublicId }}</span>
          </div>
        </div>
        <button mat-icon-button mat-dialog-close>
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="dialog-content">
        <app-loading-indicator *ngIf="isLoading()" message="Loading application details..."></app-loading-indicator>

        <ng-container *ngIf="!isLoading() && application() as app">
          <!-- Status Banner -->
          <div class="status-banner" [ngClass]="'status-' + app.status.toLowerCase()">
            <span class="status-text">Status: <strong>{{ app.status }}</strong></span>
            <span *ngIf="app.status === 'PENDING'" class="step-info">
              Step {{ app.currentStepNumber }} of {{ app.requiredStepsCount }}
            </span>
            <span *ngIf="app.finalRejectionReason" class="rejection-text">
              Reason: {{ app.finalRejectionReason }}
            </span>
          </div>

          <!-- Employee & Leave Details Grid -->
          <div class="details-grid">
            <div class="detail-item">
              <span class="label">Employee</span>
              <span class="value font-medium">{{ app.employeeName }}</span>
              <span class="sub-value">{{ app.employeeNumber }} • {{ app.departmentName || 'No Department' }}</span>
            </div>

            <div class="detail-item">
              <span class="label">Leave Type</span>
              <span class="value font-medium">{{ app.leaveTypeName }} ({{ app.leaveTypeCode }})</span>
            </div>

            <div class="detail-item">
              <span class="label">Duration</span>
              <span class="value font-medium">{{ app.startDate }} to {{ app.endDate }}</span>
              <span class="sub-value">
                {{ app.totalDays }} day(s)
                <span *ngIf="app.halfDay" class="badge-half">({{ app.halfDayPeriod || 'Half Day' }})</span>
              </span>
            </div>

            <div class="detail-item">
              <span class="label">Applied On</span>
              <span class="value">{{ app.appliedAt | date:'medium' }}</span>
            </div>

            <div class="detail-item full-width">
              <span class="label">Reason</span>
              <p class="reason-box">{{ app.reason }}</p>
            </div>

            <div *ngIf="app.contactPhoneDuringLeave" class="detail-item">
              <span class="label">Contact Phone During Leave</span>
              <span class="value">{{ app.contactPhoneDuringLeave }}</span>
            </div>

            <div *ngIf="app.documentStorageKey" class="detail-item">
              <span class="label">Supporting Document</span>
              <button mat-stroked-button color="primary" class="download-btn" (click)="downloadDoc(app.publicId)">
                <mat-icon>download</mat-icon>
                Download Attachment
              </button>
            </div>
          </div>

          <mat-divider class="my-4"></mat-divider>

          <!-- Approval Steps Timeline -->
          <h3 class="section-title">
            <mat-icon>timeline</mat-icon> Approval History & Workflow
          </h3>

          <div class="timeline">
            <div
              *ngFor="let step of app.approvals"
              class="timeline-item"
              [ngClass]="'step-' + step.status.toLowerCase()"
            >
              <div class="timeline-marker">
                <mat-icon *ngIf="step.status === 'APPROVED'">check_circle</mat-icon>
                <mat-icon *ngIf="step.status === 'REJECTED'">cancel</mat-icon>
                <mat-icon *ngIf="step.status === 'PENDING'">hourglass_empty</mat-icon>
                <mat-icon *ngIf="step.status === 'CANCELLED'">block</mat-icon>
              </div>
              <div class="timeline-content">
                <div class="step-header">
                  <span class="step-title">Step {{ step.stepNumber }}: {{ formatApproverRole(step.approverRole) }}</span>
                  <span class="step-status" [ngClass]="'badge-' + step.status.toLowerCase()">
                    {{ step.status }}
                  </span>
                </div>
                <div class="step-details">
                  <span *ngIf="step.assignedApproverName" class="approver-info">
                    Assigned Approver: {{ step.assignedApproverName }}
                  </span>
                  <span *ngIf="step.actionTakenByUsername" class="actor-info">
                    Action taken by: {{ step.actionTakenByUsername }}
                  </span>
                  <span *ngIf="step.decisionTimestamp" class="timestamp">
                    on {{ step.decisionTimestamp | date:'medium' }}
                  </span>
                </div>
                <p *ngIf="step.comments" class="step-comment">
                  "{{ step.comments }}"
                </p>
                <p *ngIf="step.rejectionReason" class="step-comment text-danger">
                  Rejection Reason: "{{ step.rejectionReason }}"
                </p>
              </div>
            </div>
          </div>
        </ng-container>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-button mat-dialog-close>Close</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-container { padding: 8px 16px; max-width: 650px; }
    .dialog-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .header-title { display: flex; align-items: center; gap: 12px; }
    .header-icon { font-size: 28px; width: 28px; height: 28px; color: #1976d2; }
    .title-text { margin: 0; font-size: 1.25rem; font-weight: 600; }
    .app-number { font-size: 0.8rem; color: #666; font-family: monospace; }
    .status-banner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px;
      border-radius: 8px;
      margin-bottom: 16px;
      font-size: 0.95rem;
    }
    .status-pending { background-color: #fff8e1; color: #b78103; border: 1px solid #ffe082; }
    .status-approved { background-color: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }
    .status-rejected { background-color: #ffebee; color: #c62828; border: 1px solid #ffcdd2; }
    .status-cancelled { background-color: #f5f5f5; color: #616161; border: 1px solid #e0e0e0; }
    .details-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .detail-item { display: flex; flex-direction: column; }
    .detail-item.full-width { grid-column: span 2; }
    .label { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.5px; color: #757575; margin-bottom: 2px; }
    .value { font-size: 0.95rem; color: #212121; }
    .sub-value { font-size: 0.8rem; color: #616161; }
    .reason-box {
      margin: 4px 0 0 0;
      padding: 8px 12px;
      background-color: #f5f5f5;
      border-radius: 6px;
      font-size: 0.9rem;
      color: #333;
      border-left: 3px solid #1976d2;
    }
    .section-title { display: flex; align-items: center; gap: 8px; font-size: 1rem; font-weight: 600; margin: 16px 0 12px 0; color: #37474f; }
    .timeline { display: flex; flex-direction: column; gap: 12px; border-left: 2px solid #e0e0e0; margin-left: 14px; padding-left: 16px; }
    .timeline-item { position: relative; }
    .timeline-marker { position: absolute; left: -27px; top: 0; background: white; }
    .step-approved .timeline-marker mat-icon { color: #2e7d32; }
    .step-rejected .timeline-marker mat-icon { color: #c62828; }
    .step-pending .timeline-marker mat-icon { color: #f57c00; }
    .step-cancelled .timeline-marker mat-icon { color: #757575; }
    .timeline-content { background: #fafafa; padding: 8px 12px; border-radius: 6px; border: 1px solid #eeeeee; }
    .step-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
    .step-title { font-weight: 600; font-size: 0.9rem; }
    .step-status { font-size: 0.75rem; padding: 2px 8px; border-radius: 12px; font-weight: 600; }
    .badge-approved { background: #e8f5e9; color: #2e7d32; }
    .badge-rejected { background: #ffebee; color: #c62828; }
    .badge-pending { background: #fff8e1; color: #b78103; }
    .badge-cancelled { background: #f5f5f5; color: #616161; }
    .step-details { display: flex; flex-direction: column; font-size: 0.8rem; color: #616161; gap: 2px; }
    .step-comment { margin: 6px 0 0 0; font-style: italic; font-size: 0.85rem; color: #424242; }
    .text-danger { color: #c62828; }
    .download-btn { margin-top: 4px; align-self: flex-start; }
    .badge-half { color: #1976d2; font-weight: 500; }
  `],
})
export class LeaveDetailDialogComponent implements OnInit {
  readonly data: LeaveDetailDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<LeaveDetailDialogComponent>);
  private readonly leaveService = inject(LeaveService);
  private readonly notificationService = inject(NotificationService);

  readonly isLoading = signal(false);
  readonly application = signal<LeaveApplication | null>(null);

  ngOnInit(): void {
    if (this.data.initialApplication) {
      this.application.set(this.data.initialApplication);
      return;
    }

    this.isLoading.set(true);
    const req$ = this.data.selfService
      ? this.leaveService.getMyApplication(this.data.applicationPublicId)
      : this.leaveService.getApplication(this.data.applicationPublicId);

    req$.subscribe({
      next: (app) => {
        this.application.set(app);
        this.isLoading.set(false);
      },
      error: () => {
        this.notificationService.error('Failed to load leave application details');
        this.isLoading.set(false);
      },
    });
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

  downloadDoc(publicId: string): void {
    const req$ = this.data.selfService
      ? this.leaveService.downloadMyApplicationDocument(publicId)
      : this.leaveService.downloadApplicationDocument(publicId);

    req$.subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `leave_document_${publicId}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.notificationService.error('Failed to download document');
      },
    });
  }
}
