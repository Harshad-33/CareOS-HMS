import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { HospitalBranch, HospitalBranchRequest } from '../../models/hospital.model';

export interface BranchDialogData {
  branch?: HospitalBranch;
  title: string;
}

@Component({
  selector: 'app-branch-dialog',
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
        <div class="row">
          <mat-form-field appearance="outline" class="half-width">
            <mat-label>Branch Code</mat-label>
            <input matInput formControlName="branchCode" placeholder="e.g. MAIN-01" style="text-transform: uppercase" />
            @if (form.get('branchCode')?.hasError('required')) {
              <mat-error>Branch code is required</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline" class="half-width">
            <mat-label>Branch Name</mat-label>
            <input matInput formControlName="branchName" placeholder="e.g. City Center Branch" />
            @if (form.get('branchName')?.hasError('required')) {
              <mat-error>Branch name is required</mat-error>
            }
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline" class="half-width">
            <mat-label>Contact Number</mat-label>
            <input matInput formControlName="contactNumber" placeholder="+91..." />
          </mat-form-field>

          <mat-form-field appearance="outline" class="half-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" placeholder="branch@hospital.com" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Address</mat-label>
          <input matInput formControlName="address" placeholder="Street / Building" />
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline" class="third-width">
            <mat-label>City</mat-label>
            <input matInput formControlName="city" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="third-width">
            <mat-label>State</mat-label>
            <input matInput formControlName="state" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="third-width">
            <mat-label>PIN Code</mat-label>
            <input matInput formControlName="pinCode" />
          </mat-form-field>
        </div>

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
      min-width: 480px;
    }
    .row {
      display: flex;
      gap: 12px;
    }
    .full-width { width: 100%; }
    .half-width { flex: 1; }
    .third-width { flex: 1; }
    `,
  ],
})
export class BranchDialogComponent {
  readonly dialogRef = inject(MatDialogRef<BranchDialogComponent>);
  readonly data = inject<BranchDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    branchCode: [
      this.data.branch?.branchCode ?? '',
      [Validators.required, Validators.pattern(/^[A-Za-z0-9_-]{2,32}$/)],
    ],
    branchName: [this.data.branch?.branchName ?? '', [Validators.required, Validators.maxLength(100)]],
    contactNumber: [this.data.branch?.contactNumber ?? '', [Validators.maxLength(32)]],
    email: [this.data.branch?.email ?? '', [Validators.email, Validators.maxLength(160)]],
    address: [this.data.branch?.address ?? '', [Validators.maxLength(255)]],
    city: [this.data.branch?.city ?? '', [Validators.maxLength(100)]],
    district: [this.data.branch?.district ?? '', [Validators.maxLength(100)]],
    state: [this.data.branch?.state ?? '', [Validators.maxLength(100)]],
    country: [this.data.branch?.country ?? 'India', [Validators.maxLength(100)]],
    pinCode: [this.data.branch?.pinCode ?? '', [Validators.maxLength(20)]],
    status: [this.data.branch?.status ?? 'ACTIVE', [Validators.required]],
  });

  save(): void {
    if (this.form.invalid) return;
    const val = this.form.value;
    const req: HospitalBranchRequest = {
      branchCode: val.branchCode.toUpperCase().trim(),
      branchName: val.branchName.trim(),
      contactNumber: val.contactNumber?.trim(),
      email: val.email?.trim(),
      address: val.address?.trim(),
      city: val.city?.trim(),
      district: val.district?.trim(),
      state: val.state?.trim(),
      country: val.country?.trim(),
      pinCode: val.pinCode?.trim(),
      status: val.status,
    };
    this.dialogRef.close(req);
  }
}