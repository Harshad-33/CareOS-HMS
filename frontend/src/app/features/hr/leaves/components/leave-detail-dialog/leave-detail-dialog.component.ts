import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { LeaveApplication } from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';
import { NotificationService } from '../../../../../core/services/notification.service';

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
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-title">
          <mat-icon class="header-icon">event_note</mat-icon>
          <div>
            <h2 mat-dialog-title class="title-text">Leave Application Details</h2>
            <span class="app-number">{{ data.applicationNumber }}</span>
          </div>
        </div>
        <button mat-icon-button mat-dialog-close>
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="dialog-content">
        <!-- Status Banner -->
        <div class="status-banner" [ngClass]="'status-' + data.status.toLowerCase()">
          <span class="status-text">Status: <strong>{{ data.status }}</strong></span>
          <span *ngIf="data.status === 'PENDING'" class="step-info">
            Step {{ data.currentStepNumber }} of {{ data.requiredStepsCount }}
          </span>
          <span *ngIf="data.finalRejectionReason" class="rejection-text">
            Reason: {{ data.finalRejectionReason }}
          </span>
        </div>

        <!-- Employee & Leave Details Grid -->
        <div class="details-grid">
          <div class="detail-item">
            <span class="label">Employee</span>
            <span class="value font-medium">{{ data.employeeName }}</span>
            <span class="sub-value">{{ data.employeeNumber }} • {{ data.departmentName || 'No Department' }}</span>
          </div>

          <div class="detail-item">
            <span class="label">Leave Type</span>
            <span class="value font-medium">{{ data.leaveTypeName }} ({{ data.leaveTypeCode }})</span>
          </div>

          <div class="detail-item">
            <span class="label">Duration</span>
            <span class="value font-medium">{{ data.startDate }} to {{ data.endDate }}</span>
            <span class="sub-value">
              {{ data.totalDays }} day(s)
              <span *ngIf="data.halfDay" class="badge-half">({{ data.halfDayPeriod }})</span>
            </span>
          </div>

          <div class="detail-item">
            <span class="label">Applied On</span>
            <span class="value">{{ data.appliedAt | date:'medium' }}</span>
          </div>

          <div class="detail-item full-width">
            <span class="label">Reason</span>
            <p class="reason-box">{{ data.reason }}</p>
          </div>

          <div *ngIf="data.emergencyContactPhone" class="detail-item">
            <span class="label">Emergency Contact</span>
            <span class="value">{{ data.emergencyContactPhone }}</span>
          </div>

          <div *ngIf="data.hasDocument" class="detail-item">
            <span class="label">Supporting Document</span>
            <button mat-stroked-button color="primary" class="download-btn" (click)="downloadDoc()">
              <mat-icon>download</mat-icon>
              {{ data.documentOriginalFilename || 'Download Document' }}
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
            *ngFor="let step of data.approvalSteps; let last = last"
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
                <span class="step-title">Step {{ step.stepNumber }}: {{ step.stepName }}</span>
                <span class="step-status" [ngClass]="'badge-' + step.status.toLowerCase()">
                  {{ step.status }}
                </span>
              </div>
              <div class="step-details">
                <span *ngIf="step.approverName" class="approver-info">
                  Assigned Approver: {{ step.approverName }}
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
            </div>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-button mat-dialog-close>Close</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-container {
      padding: 8px 16px;
      max-width: 650px;
    }
    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    .header-title {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .header-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
      color: #1976d2;
    }
    .title-text {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 600;
    }
    .app-number {
      font-size: 0.85rem;
      color: #666;
    }
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

    .details-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .detail-item {
      display: flex;
      flex-direction: column;
    }
    .detail-item.full-width {
      grid-column: span 2;
    }
    .label {
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #757575;
      margin-bottom: 2px;
    }
    .value {
      font-size: 0.95rem;
      color: #212121;
    }
    .sub-value {
      font-size: 0.8rem;
      color: #616161;
    }
    .reason-box {
      margin: 4px 0 0 0;
      padding: 8px 12px;
      background-color: #f5f5f5;
      border-radius: 6px;
      font-size: 0.9rem;
      color: #333;
      border-left: 3px solid #1976d2;
    }
    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1rem;
      font-weight: 600;
      margin: 16px 0 12px 0;
      color: #37474f;
    }
    .timeline {
      display: flex;
      flex-direction: column;
      gap: 12px;
      border-left: 2px solid #e0e0e0;
      margin-left: 14px;
      padding-left: 16px;
    }
    .timeline-item {
      position: relative;
    }
    .timeline-marker {
      position: absolute;
      left: -27px;
      top: 0;
      background: white;
    }
    .step-approved .timeline-marker mat-icon { color: #2e7d32; }
    .step-rejected .timeline-marker mat-icon { color: #c62828; }
    .step-pending .timeline-marker mat-icon { color: #f57c00; }
    .step-cancelled .timeline-marker mat-icon { color: #757575; }

    .timeline-content {
      background: #fafafa;
      padding: 8px 12px;
      border-radius: 6px;
      border: 1px solid #eeeeee;
    }
    .step-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;
    }
    .step-title {
      font-weight: 600;
      font-size: 0.9rem;
    }
    .step-status {
      font-size: 0.75rem;
      padding: 2px 8px;
      border-radius: 12px;
      font-weight: 600;
    }
    .badge-approved { background: #e8f5e9; color: #2e7d32; }
    .badge-rejected { background: #ffebee; color: #c62828; }
    .badge-pending { background: #fff8e1; color: #b78103; }
    .badge-cancelled { background: #f5f5f5; color: #616161; }
    .step-details {
      display: flex;
      flex-direction: column;
      font-size: 0.8rem;
      color: #616161;
      gap: 2px;
    }
    .step-comment {
      margin: 6px 0 0 0;
      font-style: italic;
      font-size: 0.85rem;
      color: #424242;
    }
    .download-btn {
      margin-top: 4px;
      align-self: flex-start;
    }
    .badge-half {
      color: #1976d2;
      font-weight: 500;
    }
  `],
})
export class LeaveDetailDialogComponent {
  readonly data: LeaveApplication = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<LeaveDetailDialogComponent>);
  private readonly leaveService = inject(LeaveService);
  private readonly notificationService = inject(NotificationService);

  downloadDoc(): void {
    this.leaveService.downloadApplicationDocument(this.data.publicId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.data.documentOriginalFilename || 'leave_document';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.notificationService.error('Failed to download document');
      },
    });
  }
}
