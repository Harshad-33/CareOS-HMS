import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Department, DepartmentRequest } from '../../models/hospital.model';

export interface DepartmentDialogData {
  department?: Department;
  title: string;
}

@Component({
  selector: 'app-department-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Department Code</mat-label>
          <input matInput formControlName="departmentCode" placeholder="e.g. CARDIO" style="text-transform: uppercase" />
          @if (form.get('departmentCode')?.hasError('required')) {
            <mat-error>Department code is required</mat-error>
          }
          @if (form.get('departmentCode')?.hasError('pattern')) {
            <mat-error>Alphanumeric uppercase (2-32 chars)</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Department Name</mat-label>
          <input matInput formControlName="departmentName" placeholder="e.g. Cardiology" />
          @if (form.get('departmentName')?.hasError('required')) {
            <mat-error>Department name is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3" placeholder="Brief description of department scope"></textarea>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Status</mat-label>
          <mat-select formControlName="status">
            <mat-option value="ACTIVE">ACTIVE</mat-option>
            <mat-option value="INACTIVE">INACTIVE</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">Save</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
    .dialog-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-top: 8px;
      min-width: 360px;
    }
    .full-width { width: 100%; }
    `,
  ],
})
export class DepartmentDialogComponent {
  readonly dialogRef = inject(MatDialogRef<DepartmentDialogComponent>);
  readonly data = inject<DepartmentDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    departmentCode: [
      this.data.department?.departmentCode ?? '',
      [Validators.required, Validators.pattern(/^[A-Za-z0-9_-]{2,32}$/)],
    ],
    departmentName: [this.data.department?.departmentName ?? '', [Validators.required, Validators.maxLength(100)]],
    description: [this.data.department?.description ?? '', [Validators.maxLength(255)]],
    status: [this.data.department?.status ?? 'ACTIVE', [Validators.required]],
  });

  save(): void {
    if (this.form.invalid) return;
    const val = this.form.value;
    const req: DepartmentRequest = {
      departmentCode: val.departmentCode.toUpperCase().trim(),
      departmentName: val.departmentName.trim(),
      description: val.description?.trim(),
      status: val.status,
    };
    this.dialogRef.close(req);
  }
}