import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { CreateLeaveTypeRequest, LeaveApprovalWorkflowType, LeaveType, UpdateLeaveTypeRequest } from '../../models/leave.models';

export interface LeaveTypeDialogData {
  leaveType?: LeaveType;
}

@Component({
  selector: 'app-leave-type-dialog',
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
    MatIconModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <mat-icon color="primary">{{ isEdit ? 'edit' : 'add_box' }}</mat-icon>
        <h2 mat-dialog-title>{{ isEdit ? 'Edit Leave Policy' : 'Create New Leave Policy' }}</h2>
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-dialog-content class="dialog-content">
          <div class="form-row">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Leave Code</mat-label>
              <input
                matInput
                formControlName="code"
                placeholder="e.g. CASUAL, SICK"
                (input)="onCodeInput($event)"
              />
              <mat-error *ngIf="form.get('code')?.hasError('required')">Code is required</mat-error>
              <mat-error *ngIf="form.get('code')?.hasError('pattern')">
                2-32 uppercase alphanumeric / underscore
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="flex-2">
              <mat-label>Policy Name</mat-label>
              <input matInput formControlName="name" placeholder="e.g. Casual Leave" />
              <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Description (Optional)</mat-label>
            <textarea matInput rows="2" formControlName="description" placeholder="Policy details..."></textarea>
          </mat-form-field>

          <div class="form-row">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Annual Entitlement (Days)</mat-label>
              <input matInput type="number" formControlName="annualAllowance" min="0" max="365" />
              <mat-error *ngIf="form.get('annualAllowance')?.hasError('required')">Required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Max Consecutive Days</mat-label>
              <input matInput type="number" formControlName="maxConsecutiveDays" min="0" max="90" />
            </mat-form-field>
          </div>

          <div class="form-row">
            <mat-form-field appearance="outline" class="flex-2">
              <mat-label>Approval Workflow</mat-label>
              <mat-select formControlName="approvalWorkflow" (selectionChange)="onWorkflowChange($event.value)">
                <mat-option value="HR_ONLY">HR Admin Only (1 Level)</mat-option>
                <mat-option value="MANAGER_ONLY">Direct Reporting Manager Only (1 Level)</mat-option>
                <mat-option value="MANAGER_THEN_HR">Manager then HR (2 Levels)</mat-option>
                <mat-option value="DEPT_HEAD_THEN_HR">Department Head then HR (2 Levels)</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Approval Levels</mat-label>
              <input matInput type="number" formControlName="approvalLevelsCount" readonly />
            </mat-form-field>
          </div>

          <div class="checkbox-group">
            <mat-checkbox formControlName="allowHalfDay">Allow Half-Day Applications</mat-checkbox>
            <mat-checkbox formControlName="requiresDocument">Require Supporting / Medical Certificate</mat-checkbox>
            <mat-checkbox formControlName="carryForwardAllowed">Allow Year-End Carry Forward</mat-checkbox>
          </div>

          <div *ngIf="form.get('carryForwardAllowed')?.value" class="form-row mt-2">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Max Carry Forward Limit (Days)</mat-label>
              <input matInput type="number" formControlName="maxCarryForwardDays" min="0" max="60" />
            </mat-form-field>
          </div>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" mat-dialog-close>Cancel</button>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
            {{ isEdit ? 'Save Changes' : 'Create Policy' }}
          </button>
        </mat-dialog-actions>
      </form>
    </div>
  `,
  styles: [`
    .dialog-container {
      padding: 8px 16px;
      min-width: 500px;
      max-width: 600px;
    }
    .dialog-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }
    .dialog-header h2 {
      margin: 0;
      font-size: 1.25rem;
    }
    .dialog-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .form-row {
      display: flex;
      gap: 12px;
    }
    .flex-1 { flex: 1; }
    .flex-2 { flex: 2; }
    .w-full { width: 100%; }
    .checkbox-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
      background: #f9f9f9;
      padding: 10px 14px;
      border-radius: 6px;
      margin: 6px 0;
    }
    .mt-2 { margin-top: 8px; }
  `],
})
export class LeaveTypeDialogComponent implements OnInit {
  readonly data: LeaveTypeDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<LeaveTypeDialogComponent, CreateLeaveTypeRequest | UpdateLeaveTypeRequest>);
  private readonly fb = inject(FormBuilder);

  isEdit = false;
  form!: FormGroup;

  ngOnInit(): void {
    this.isEdit = !!this.data?.leaveType;
    const item = this.data?.leaveType;

    this.form = this.fb.group({
      code: [
        { value: item?.code || '', disabled: this.isEdit },
        [Validators.required, Validators.pattern(/^[A-Z0-9_]{2,32}$/)],
      ],
      name: [item?.name || '', [Validators.required, Validators.maxLength(100)]],
      description: [item?.description || '', [Validators.maxLength(255)]],
      annualAllowance: [item?.annualAllowance ?? 12, [Validators.required, Validators.min(0), Validators.max(365)]],
      maxConsecutiveDays: [item?.maxConsecutiveDays ?? 0, [Validators.min(0), Validators.max(90)]],
      carryForwardAllowed: [item?.carryForwardAllowed ?? false],
      maxCarryForwardDays: [item?.maxCarryForwardDays ?? 0, [Validators.min(0), Validators.max(60)]],
      requiresDocument: [item?.requiresDocument ?? false],
      allowHalfDay: [item?.allowHalfDay ?? true],
      approvalWorkflow: [item?.approvalWorkflow || 'MANAGER_THEN_HR', [Validators.required]],
      approvalLevelsCount: [{ value: item?.approvalLevelsCount ?? 2, disabled: false }, [Validators.required]],
    });

    if (item) {
      this.onWorkflowChange(item.approvalWorkflow);
    }
  }

  onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input) {
      input.value = input.value.toUpperCase();
      this.form.get('code')?.setValue(input.value);
    }
  }

  onWorkflowChange(workflow: LeaveApprovalWorkflowType): void {
    const count = workflow === 'HR_ONLY' || workflow === 'MANAGER_ONLY' ? 1 : 2;
    this.form.get('approvalLevelsCount')?.setValue(count);
  }

  submit(): void {
    if (this.form.invalid) return;
    const raw = this.form.getRawValue();

    if (this.isEdit) {
      const updateReq: UpdateLeaveTypeRequest = {
        name: raw.name.trim(),
        description: raw.description ? raw.description.trim() : undefined,
        annualAllowance: Number(raw.annualAllowance),
        carryForwardAllowed: Boolean(raw.carryForwardAllowed),
        maxCarryForwardDays: raw.carryForwardAllowed ? Number(raw.maxCarryForwardDays) : 0,
        requiresDocument: Boolean(raw.requiresDocument),
        maxConsecutiveDays: Number(raw.maxConsecutiveDays) || 0,
        allowHalfDay: Boolean(raw.allowHalfDay),
        approvalWorkflow: raw.approvalWorkflow,
        approvalLevelsCount: Number(raw.approvalLevelsCount),
      };
      this.dialogRef.close(updateReq);
    } else {
      const createReq: CreateLeaveTypeRequest = {
        code: raw.code.trim().toUpperCase(),
        name: raw.name.trim(),
        description: raw.description ? raw.description.trim() : undefined,
        annualAllowance: Number(raw.annualAllowance),
        carryForwardAllowed: Boolean(raw.carryForwardAllowed),
        maxCarryForwardDays: raw.carryForwardAllowed ? Number(raw.maxCarryForwardDays) : 0,
        requiresDocument: Boolean(raw.requiresDocument),
        maxConsecutiveDays: Number(raw.maxConsecutiveDays) || 0,
        allowHalfDay: Boolean(raw.allowHalfDay),
        approvalWorkflow: raw.approvalWorkflow,
        approvalLevelsCount: Number(raw.approvalLevelsCount),
      };
      this.dialogRef.close(createReq);
    }
  }
}
