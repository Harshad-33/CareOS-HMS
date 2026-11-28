import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { EmployeeSummary } from '../../../../employees/models/employee.model';
import {
  AdjustLeaveBalanceRequest,
  AllocateLeaveBalanceRequest,
  LeaveBalance,
  LeaveType,
} from '../../models/leave.models';

export type BalanceDialogMode = 'ALLOCATE' | 'ADJUST';

export interface LeaveBalanceDialogData {
  mode: BalanceDialogMode;
  balance?: LeaveBalance;
  leaveTypes?: LeaveType[];
  employees?: EmployeeSummary[];
}

@Component({
  selector: 'app-leave-balance-dialog',
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
    MatIconModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <mat-icon color="primary">{{ isAdjust ? 'tune' : 'account_balance_wallet' }}</mat-icon>
        <h2 mat-dialog-title>{{ isAdjust ? 'Manual Balance Adjustment' : 'Allocate Leave Balance' }}</h2>
      </div>

      <!-- Existing Balance Summary if Adjusting -->
      <div *ngIf="isAdjust && data.balance" class="balance-summary-box">
        <div class="summary-item">
          <span class="label">Employee:</span>
          <span class="value">{{ data.balance.employeeName }} ({{ data.balance.employeeNumber }})</span>
        </div>
        <div class="summary-item">
          <span class="label">Leave Type:</span>
          <span class="value">{{ data.balance.leaveTypeName }}</span>
        </div>
        <div class="summary-item">
          <span class="label">Current Available:</span>
          <span class="value font-bold text-primary">{{ data.balance.availableBalance }} days</span>
        </div>
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-dialog-content class="dialog-content">
          <!-- Allocate Mode Fields -->
          <ng-container *ngIf="!isAdjust">
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Select Employee</mat-label>
              <mat-select formControlName="employeePublicId">
                <mat-option *ngFor="let emp of data.employees" [value]="emp.publicId">
                  {{ emp.fullName }} ({{ emp.employeeNumber }} - {{ emp.designation }})
                </mat-option>
              </mat-select>
              <mat-error *ngIf="form.get('employeePublicId')?.hasError('required')">
                Employee is required
              </mat-error>
            </mat-form-field>

            <div class="form-row">
              <mat-form-field appearance="outline" class="flex-2">
                <mat-label>Leave Type</mat-label>
                <mat-select formControlName="leaveTypePublicId">
                  <mat-option *ngFor="let lt of data.leaveTypes" [value]="lt.publicId">
                    {{ lt.name }} ({{ lt.annualAllowance }} days/yr)
                  </mat-option>
                </mat-select>
                <mat-error *ngIf="form.get('leaveTypePublicId')?.hasError('required')">
                  Leave type is required
                </mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="flex-1">
                <mat-label>Calendar Year</mat-label>
                <input matInput type="number" formControlName="calendarYear" min="2020" max="2100" />
                <mat-error *ngIf="form.get('calendarYear')?.hasError('required')">Required</mat-error>
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Days to Allocate</mat-label>
              <input matInput type="number" formControlName="allocatedDays" min="0" max="365" step="0.5" />
              <mat-error *ngIf="form.get('allocatedDays')?.hasError('required')">
                Allocated days is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Allocation Remarks (Optional)</mat-label>
              <textarea
                matInput
                rows="2"
                formControlName="remarks"
                placeholder="e.g. Initial annual entitlement"
              ></textarea>
            </mat-form-field>
          </ng-container>

          <!-- Adjust Mode Fields -->
          <ng-container *ngIf="isAdjust">
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Adjustment Days (+ to credit, - to debit)</mat-label>
              <input matInput type="number" formControlName="adjustmentDays" step="0.5" />
              <mat-hint>Use negative numbers (e.g. -2) to deduct leave days</mat-hint>
              <mat-error *ngIf="form.get('adjustmentDays')?.hasError('required')">
                Adjustment days is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Adjustment Reason (Mandatory)</mat-label>
              <textarea
                matInput
                rows="2"
                formControlName="reason"
                placeholder="e.g. Compensation for overtime, correction"
              ></textarea>
              <mat-error *ngIf="form.get('reason')?.hasError('required')">
                Reason is required for manual adjustments
              </mat-error>
            </mat-form-field>
          </ng-container>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" mat-dialog-close>Cancel</button>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
            {{ isAdjust ? 'Apply Adjustment' : 'Allocate Balance' }}
          </button>
        </mat-dialog-actions>
      </form>
    </div>
  `,
  styles: [`
    .dialog-container { padding: 8px 16px; min-width: 440px; max-width: 520px; }
    .dialog-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
    .dialog-header h2 { margin: 0; font-size: 1.2rem; }
    .balance-summary-box {
      background: #e3f2fd;
      border: 1px solid #bbdefb;
      border-radius: 6px;
      padding: 10px 14px;
      margin-bottom: 12px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .summary-item { display: flex; justify-content: space-between; font-size: 0.85rem; }
    .summary-item .label { color: #555; }
    .summary-item .value { color: #111; font-weight: 500; }
    .form-row { display: flex; gap: 12px; }
    .flex-1 { flex: 1; }
    .flex-2 { flex: 2; }
    .w-full { width: 100%; margin-bottom: 4px; }
    .text-primary { color: #1976d2; }
    .font-bold { font-weight: 700; }
  `],
})
export class LeaveBalanceDialogComponent implements OnInit {
  readonly data: LeaveBalanceDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(
    MatDialogRef<LeaveBalanceDialogComponent, AllocateLeaveBalanceRequest | AdjustLeaveBalanceRequest>
  );
  private readonly fb = inject(FormBuilder);

  isAdjust = false;
  form!: FormGroup;

  ngOnInit(): void {
    this.isAdjust = this.data.mode === 'ADJUST';
    const currentYear = new Date().getFullYear();

    if (this.isAdjust) {
      this.form = this.fb.group({
        adjustmentDays: [null, [Validators.required]],
        reason: ['', [Validators.required, Validators.maxLength(255)]],
      });
    } else {
      this.form = this.fb.group({
        employeePublicId: ['', [Validators.required]],
        leaveTypePublicId: ['', [Validators.required]],
        calendarYear: [currentYear, [Validators.required, Validators.min(2020), Validators.max(2100)]],
        allocatedDays: [null, [Validators.required, Validators.min(0), Validators.max(365)]],
        remarks: ['', [Validators.maxLength(255)]],
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    const value = this.form.value;

    if (this.isAdjust) {
      const adjustReq: AdjustLeaveBalanceRequest = {
        adjustmentDays: Number(value.adjustmentDays),
        reason: value.reason.trim(),
      };
      this.dialogRef.close(adjustReq);
    } else {
      const allocReq: AllocateLeaveBalanceRequest = {
        employeePublicId: value.employeePublicId,
        leaveTypePublicId: value.leaveTypePublicId,
        calendarYear: Number(value.calendarYear),
        allocatedDays: Number(value.allocatedDays),
        remarks: value.remarks ? value.remarks.trim() : undefined,
      };
      this.dialogRef.close(allocReq);
    }
  }
}
