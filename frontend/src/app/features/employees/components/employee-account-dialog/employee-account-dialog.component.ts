import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { EmployeeSummary, LinkUserAccountRequest } from '../../models/employee.model';

export interface EmployeeAccountDialogData {
  employee: EmployeeSummary;
}

export type EmployeeAccountDialogResult =
  | { action: 'link'; request: LinkUserAccountRequest }
  | { action: 'unlink' };

@Component({
  selector: 'app-employee-account-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="dialog-title-icon">manage_accounts</mat-icon>
      User Account: {{ data.employee.fullName }}
    </h2>
    <mat-dialog-content>
      @if (data.employee.hasUserAccount) {
        <div class="linked-info-box">
          <p class="status-message">
            <mat-icon color="primary">check_circle</mat-icon>
            This employee is linked to active login credentials:
          </p>
          <div class="user-details">
            <div><strong>Username:</strong> {{ data.employee.username }}</div>
            <div><strong>Email:</strong> {{ data.employee.email }}</div>
          </div>
          <p class="unlink-warning">
            Unlinking this account will immediately revoke all active login sessions and detach the user account from this employee profile.
          </p>
        </div>
      } @else {
        <form [formGroup]="form" class="dialog-form">
          <p class="form-instructions">
            Provision a login account for <strong>{{ data.employee.fullName }}</strong> so they can sign in to CareOS.
          </p>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Username</mat-label>
            <input matInput formControlName="username" placeholder="e.g. dr.johndoe" />
            <mat-icon matPrefix>person</mat-icon>
            @if (form.get('username')?.hasError('required')) {
              <mat-error>Username is required</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput type="password" formControlName="password" placeholder="Min 12 chars (Upper, Lower, Digit, Special)" />
            <mat-icon matPrefix>lock</mat-icon>
            @if (form.get('password')?.hasError('required')) {
              <mat-error>Password is required</mat-error>
            }
            @if (form.get('password')?.hasError('minlength')) {
              <mat-error>Password must be at least 12 characters</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Assign Role(s)</mat-label>
            <mat-select formControlName="roleCodes" multiple>
              <mat-option value="HOSPITAL_ADMIN">Hospital Administrator</mat-option>
            </mat-select>
            <mat-hint>Select roles for permissions within this hospital</mat-hint>
          </mat-form-field>
        </form>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>

      @if (data.employee.hasUserAccount) {
        <button mat-flat-button color="warn" (click)="unlink()">
          <mat-icon>link_off</mat-icon>
          Unlink Account
        </button>
      } @else {
        <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">
          <mat-icon>person_add</mat-icon>
          Create & Link Account
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-title-icon {
        vertical-align: middle;
        margin-right: 8px;
      }
      .dialog-form {
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding-top: 8px;
        min-width: 420px;
      }
      .full-width {
        width: 100%;
      }
      .form-instructions {
        font-size: 14px;
        color: #555;
        margin-bottom: 8px;
      }
      .linked-info-box {
        padding: 16px;
        background: #f8f9fa;
        border: 1px solid #e9ecef;
        border-radius: 6px;
        min-width: 400px;
      }
      .status-message {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
        margin-bottom: 12px;
      }
      .user-details {
        display: flex;
        flex-direction: column;
        gap: 6px;
        font-size: 14px;
        background: #fff;
        padding: 12px;
        border-radius: 4px;
        border: 1px solid #dee2e6;
        margin-bottom: 12px;
      }
      .unlink-warning {
        font-size: 13px;
        color: #d32f2f;
        margin: 0;
      }
    `,
  ],
})
export class EmployeeAccountDialogComponent implements OnInit {
  readonly dialogRef = inject(MatDialogRef<EmployeeAccountDialogComponent>);
  readonly data = inject<EmployeeAccountDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(64)]],
    password: ['', [Validators.required, Validators.minLength(12)]],
    roleCodes: [['HOSPITAL_ADMIN']],
  });

  ngOnInit(): void {
    if (!this.data.employee.hasUserAccount) {
      // Suggest username based on employee name: firstname.lastname
      const suggested = (
        this.data.employee.firstName + '.' + this.data.employee.lastName
      )
        .toLowerCase()
        .replace(/[^a-z0-9._]/g, '');
      this.form.patchValue({ username: suggested });
    }
  }

  save(): void {
    if (this.form.invalid) return;
    const val = this.form.value;
    const req: LinkUserAccountRequest = {
      username: val.username.trim(),
      password: val.password,
      roleCodes: val.roleCodes,
    };
    this.dialogRef.close({ action: 'link', request: req } as EmployeeAccountDialogResult);
  }

  unlink(): void {
    this.dialogRef.close({ action: 'unlink' } as EmployeeAccountDialogResult);
  }
}
