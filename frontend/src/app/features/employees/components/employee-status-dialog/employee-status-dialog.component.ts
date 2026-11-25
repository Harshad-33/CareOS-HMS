import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  EmployeeStatus,
  EmployeeSummary,
  UpdateEmployeeStatusRequest,
} from '../../models/employee.model';

export interface EmployeeStatusDialogData {
  employee: EmployeeSummary;
}

@Component({
  selector: 'app-employee-status-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>Change Status: {{ data.employee.fullName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <p class="current-status">
          Employee Number: <strong>{{ data.employee.employeeNumber }}</strong><br />
          Current Status: <span class="status-badge" [ngClass]="data.employee.status.toLowerCase()">{{ data.employee.status }}</span>
        </p>

        @if (allowedStatuses.length === 0) {
          <div class="terminal-warning">
            This employee is in a terminal status (<strong>{{ data.employee.status }}</strong>) and cannot be transitioned to another status.
          </div>
        } @else {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Target Status</mat-label>
            <mat-select formControlName="status">
              @for (st of allowedStatuses; track st.value) {
                <mat-option [value]="st.value">{{ st.label }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Effective Date</mat-label>
            <input matInput type="date" formControlName="effectiveDate" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Reason / Notes</mat-label>
            <textarea
              matInput
              rows="3"
              formControlName="reason"
              placeholder="Provide a mandatory reason for this status change (e.g., Annual leave, disciplinary action, relocation)"
            ></textarea>
          </mat-form-field>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      @if (allowedStatuses.length > 0) {
        <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">
          Update Status
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-form {
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding-top: 8px;
        min-width: 400px;
      }
      .full-width {
        width: 100%;
      }
      .current-status {
        font-size: 14px;
        line-height: 1.6;
        margin-bottom: 8px;
      }
      .status-badge {
        display: inline-block;
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: 600;
        font-size: 12px;
        text-transform: uppercase;
      }
      .status-badge.active {
        background: #e8f5e9;
        color: #2e7d32;
      }
      .status-badge.suspended {
        background: #fff3e0;
        color: #e65100;
      }
      .status-badge.inactive {
        background: #f5f5f5;
        color: #616161;
      }
      .status-badge.resigned {
        background: #ede7f6;
        color: #512da8;
      }
      .status-badge.terminated {
        background: #ffebee;
        color: #c62828;
      }
      .terminal-warning {
        padding: 12px;
        background: #fff3e0;
        border-left: 4px solid #ff9800;
        color: #e65100;
        border-radius: 4px;
        font-size: 13px;
      }
    `,
  ],
})
export class EmployeeStatusDialogComponent implements OnInit {
  readonly dialogRef = inject(MatDialogRef<EmployeeStatusDialogComponent>);
  readonly data = inject<EmployeeStatusDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  allowedStatuses: { value: EmployeeStatus; label: string }[] = [];

  readonly form: FormGroup = this.fb.group({
    status: ['', [Validators.required]],
    effectiveDate: [new Date().toISOString().split('T')[0]],
    reason: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    const current = this.data.employee.status;
    if (current === 'ACTIVE') {
      this.allowedStatuses = [
        { value: 'SUSPENDED', label: 'SUSPENDED (Temporarily locks account & access)' },
        { value: 'INACTIVE', label: 'INACTIVE (On extended leave or inactive)' },
        { value: 'RESIGNED', label: 'RESIGNED (Voluntary resignation - Final)' },
        { value: 'TERMINATED', label: 'TERMINATED (Employment ended - Final)' },
      ];
    } else if (current === 'SUSPENDED') {
      this.allowedStatuses = [
        { value: 'ACTIVE', label: 'ACTIVE (Reinstate employment & account)' },
        { value: 'RESIGNED', label: 'RESIGNED (Voluntary resignation - Final)' },
        { value: 'TERMINATED', label: 'TERMINATED (Employment ended - Final)' },
      ];
    } else if (current === 'INACTIVE') {
      this.allowedStatuses = [
        { value: 'ACTIVE', label: 'ACTIVE (Reactivate employment)' },
        { value: 'RESIGNED', label: 'RESIGNED (Voluntary resignation - Final)' },
        { value: 'TERMINATED', label: 'TERMINATED (Employment ended - Final)' },
      ];
    } else {
      this.allowedStatuses = [];
    }

    if (this.allowedStatuses.length > 0) {
      this.form.patchValue({ status: this.allowedStatuses[0].value });
    }
  }

  save(): void {
    if (this.form.invalid) return;
    const val = this.form.value;
    const req: UpdateEmployeeStatusRequest = {
      status: val.status as EmployeeStatus,
      reason: val.reason.trim(),
      effectiveDate: val.effectiveDate || undefined,
    };
    this.dialogRef.close(req);
  }
}
