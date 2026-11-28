import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NotificationService } from '../../../../../core/services/notification.service';
import {
  ApplyLeaveRequest,
  HalfDayPeriod,
  LeaveBalance,
  LeaveType,
  LeaveTypeSummary,
} from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';

export interface ApplyLeaveDialogData {
  leaveTypes: (LeaveTypeSummary | LeaveType)[];
  balances?: LeaveBalance[];
  employeePublicId?: string;
}

@Component({
  selector: 'app-apply-leave-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <mat-icon color="primary">time_to_leave</mat-icon>
        <h2 mat-dialog-title>Apply for Leave</h2>
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-dialog-content class="dialog-content">
          <!-- Leave Type Select -->
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Select Leave Type</mat-label>
            <mat-select formControlName="leaveTypePublicId" (selectionChange)="onTypeChange($event.value)">
              <mat-option *ngFor="let lt of data.leaveTypes" [value]="lt.publicId">
                {{ lt.name }} ({{ lt.code }})
                <span *ngIf="getBalanceForType(lt.publicId) !== null" class="balance-tag">
                  • Available: {{ getBalanceForType(lt.publicId) }} days
                </span>
              </mat-option>
            </mat-select>
            <mat-error *ngIf="form.get('leaveTypePublicId')?.hasError('required')">
              Leave type is required
            </mat-error>
          </mat-form-field>

          <!-- Policy Guidance Info -->
          <div *ngIf="selectedType" class="policy-note">
            <mat-icon class="note-icon">info</mat-icon>
            <div class="note-content">
              <strong>{{ selectedType.name }} Policy:</strong>
              Annual: {{ selectedType.annualAllowance }} days •
              Workflow: {{ selectedType.approvalWorkflowType }} •
              {{ selectedType.medicalCertRequired ? 'Medical Certificate Required (> 2 days)' : 'No document required' }}
            </div>
          </div>

          <!-- Date Pickers -->
          <div class="form-row">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Start Date</mat-label>
              <input matInput type="date" formControlName="startDate" (change)="calculateDays()" />
              <mat-error *ngIf="form.get('startDate')?.hasError('required')">Required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>End Date</mat-label>
              <input matInput type="date" formControlName="endDate" (change)="calculateDays()" />
              <mat-error *ngIf="form.get('endDate')?.hasError('required')">Required</mat-error>
            </mat-form-field>
          </div>

          <!-- Half Day Options -->
          <div class="half-day-section" *ngIf="selectedType">
            <mat-checkbox formControlName="halfDay" (change)="onHalfDayToggle($event.checked)">
              Half-Day Application (Single Date only)
            </mat-checkbox>

            <div *ngIf="form.get('halfDay')?.value" class="period-radios">
              <mat-radio-group formControlName="halfDayPeriod">
                <mat-radio-button value="FIRST_HALF">First Half (Morning Session)</mat-radio-button>
                <mat-radio-button value="SECOND_HALF">Second Half (Afternoon Session)</mat-radio-button>
              </mat-radio-group>
            </div>
          </div>

          <!-- Calculated Days Banner -->
          <div class="calc-days-banner" *ngIf="calculatedDays() > 0">
            <span>Requested Duration:</span>
            <strong class="days-highlight">{{ calculatedDays() }} Day(s)</strong>
          </div>

          <!-- Reason Field -->
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Reason for Leave</mat-label>
            <textarea
              matInput
              rows="3"
              formControlName="reason"
              placeholder="State the purpose of your leave..."
            ></textarea>
            <mat-error *ngIf="form.get('reason')?.hasError('required')">
              Reason is required
            </mat-error>
          </mat-form-field>

          <!-- Contact Phone During Leave -->
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Contact Phone During Leave (Optional)</mat-label>
            <input matInput formControlName="contactPhoneDuringLeave" placeholder="+91-9876543210" />
          </mat-form-field>

          <!-- Document Upload -->
          <div class="doc-upload-box" [class.doc-required]="isDocumentMandatory()">
            <label class="doc-label">
              <mat-icon>attach_file</mat-icon>
              <span>{{ isDocumentMandatory() ? 'Medical Certificate (Mandatory for > 2 days)' : 'Supporting Attachment (Optional)' }}</span>
            </label>
            <input type="file" (change)="onFileSelected($event)" accept=".pdf,.png,.jpg,.jpeg" />
            <span *ngIf="uploadedFileName" class="uploaded-name">
              Uploaded: <strong>{{ uploadedFileName }}</strong>
            </span>
            <mat-progress-bar *ngIf="isUploading()" mode="indeterminate"></mat-progress-bar>
          </div>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" mat-dialog-close>Cancel</button>
          <button
            mat-flat-button
            color="primary"
            type="submit"
            [disabled]="form.invalid || isUploading() || (isDocumentMandatory() && !documentStorageKey)"
          >
            Submit Application
          </button>
        </mat-dialog-actions>
      </form>
    </div>
  `,
  styles: [`
    .dialog-container { padding: 8px 16px; min-width: 480px; max-width: 580px; }
    .dialog-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
    .dialog-header h2 { margin: 0; font-size: 1.25rem; }
    .w-full { width: 100%; margin-bottom: 4px; }
    .form-row { display: flex; gap: 12px; }
    .flex-1 { flex: 1; }
    .balance-tag { color: #1976d2; font-size: 0.85rem; }
    .policy-note {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      background: #e8f5e9;
      border: 1px solid #c8e6c9;
      border-radius: 6px;
      padding: 8px 12px;
      margin-bottom: 12px;
      font-size: 0.85rem;
      color: #2e7d32;
    }
    .note-icon { font-size: 18px; width: 18px; height: 18px; }
    .half-day-section {
      background: #f5f5f5;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 12px;
    }
    .period-radios { margin-top: 8px; padding-left: 24px; }
    .calc-days-banner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #e3f2fd;
      border: 1px solid #90caf9;
      border-radius: 6px;
      padding: 8px 12px;
      margin-bottom: 12px;
      font-size: 0.9rem;
    }
    .days-highlight { font-size: 1.1rem; color: #1565c0; }
    .doc-upload-box {
      border: 1px dashed #bdbdbd;
      border-radius: 6px;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 6px;
      background: #fafafa;
      margin-bottom: 8px;
    }
    .doc-upload-box.doc-required { border-color: #f57c00; background: #fff8e1; }
    .doc-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.85rem;
      font-weight: 500;
      color: #424242;
    }
    .uploaded-name { font-size: 0.8rem; color: #2e7d32; }
  `],
})
export class ApplyLeaveDialogComponent implements OnInit {
  readonly data: ApplyLeaveDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<ApplyLeaveDialogComponent, ApplyLeaveRequest>);
  private readonly fb = inject(FormBuilder);
  private readonly leaveService = inject(LeaveService);
  private readonly notificationService = inject(NotificationService);

  selectedType: LeaveTypeSummary | LeaveType | null = null;
  documentStorageKey: string | null = null;
  uploadedFileName: string | null = null;
  readonly isUploading = signal(false);
  readonly calculatedDays = signal(0);

  form!: FormGroup;

  ngOnInit(): void {
    const today = new Date().toISOString().split('T')[0];

    this.form = this.fb.group({
      leaveTypePublicId: ['', [Validators.required]],
      startDate: [today, [Validators.required]],
      endDate: [today, [Validators.required]],
      halfDay: [false],
      halfDayPeriod: ['FIRST_HALF'],
      reason: ['', [Validators.required, Validators.maxLength(500)]],
      contactPhoneDuringLeave: ['', [Validators.maxLength(32)]],
    });

    if (this.data.leaveTypes?.length > 0) {
      this.onTypeChange(this.data.leaveTypes[0].publicId);
      this.form.patchValue({ leaveTypePublicId: this.data.leaveTypes[0].publicId });
    }
    this.calculateDays();
  }

  isDocumentMandatory(): boolean {
    return Boolean(this.selectedType?.medicalCertRequired) && this.calculatedDays() > 2;
  }

  onTypeChange(typePublicId: string): void {
    this.selectedType = this.data.leaveTypes.find((lt) => lt.publicId === typePublicId) || null;
    this.calculateDays();
  }

  getBalanceForType(typePublicId: string): number | null {
    const b = this.data.balances?.find((bal) => bal.leaveTypePublicId === typePublicId);
    return b ? b.availableBalance : null;
  }

  onHalfDayToggle(checked: boolean): void {
    if (checked) {
      const start = this.form.get('startDate')?.value;
      if (start) {
        this.form.get('endDate')?.setValue(start);
      }
    }
    this.calculateDays();
  }

  calculateDays(): void {
    const isHalf = this.form.get('halfDay')?.value;
    if (isHalf) {
      this.calculatedDays.set(0.5);
      return;
    }

    const startVal = this.form.get('startDate')?.value;
    const endVal = this.form.get('endDate')?.value;
    if (!startVal || !endVal) {
      this.calculatedDays.set(0);
      return;
    }

    const start = new Date(startVal);
    const end = new Date(endVal);
    if (start > end) {
      this.calculatedDays.set(0);
      return;
    }

    const diffTime = Math.abs(end.getTime() - start.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    this.calculatedDays.set(diffDays);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.isUploading.set(true);

    this.leaveService.uploadDocument(file).subscribe({
      next: (res) => {
        this.documentStorageKey = res.documentStorageKey;
        this.uploadedFileName = res.originalFilename;
        this.isUploading.set(false);
        this.notificationService.success('Document uploaded successfully');
      },
      error: () => {
        this.isUploading.set(false);
        this.notificationService.error('Failed to upload document');
      },
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    const val = this.form.value;

    const req: ApplyLeaveRequest = {
      employeePublicId: this.data.employeePublicId,
      leaveTypePublicId: val.leaveTypePublicId,
      startDate: val.startDate,
      endDate: val.halfDay ? val.startDate : val.endDate,
      halfDay: Boolean(val.halfDay),
      halfDayPeriod: val.halfDay ? (val.halfDayPeriod as HalfDayPeriod) : undefined,
      reason: val.reason.trim(),
      contactPhoneDuringLeave: val.contactPhoneDuringLeave ? val.contactPhoneDuringLeave.trim() : undefined,
      documentStorageKey: this.documentStorageKey || undefined,
    };

    this.dialogRef.close(req);
  }
}
