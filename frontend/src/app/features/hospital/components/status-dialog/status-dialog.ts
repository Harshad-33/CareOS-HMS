import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Hospital, HospitalStatus, UpdateHospitalStatusRequest } from '../../models/hospital.model';

export interface StatusDialogData {
  hospital: Hospital;
}

@Component({
  selector: 'app-status-dialog',
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
    <h2 mat-dialog-title>Change Hospital Status: {{ data.hospital.name }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <p class="current-status">Current Status: <strong>{{ data.hospital.status }}</strong></p>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Target Status</mat-label>
          <mat-select formControlName="status">
            <mat-option value="ACTIVE">ACTIVE (Operational)</mat-option>
            <mat-option value="SUSPENDED">SUSPENDED (Locks Hospital Users)</mat-option>
            <mat-option value="INACTIVE">INACTIVE (Deactivated)</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Reason / Notes</mat-label>
          <textarea
            matInput
            rows="3"
            formControlName="reason"
            placeholder="Document reason for status change (e.g. annual license renewal pending)"
          ></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">
        Update Status
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
    .dialog-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-top: 8px;
      min-width: 380px;
    }
    .full-width { width: 100%; }
    .current-status { font-size: 14px; margin-bottom: 8px; }
    `,
  ],
})
export class StatusDialogComponent {
  readonly dialogRef = inject(MatDialogRef<StatusDialogComponent>);
  readonly data = inject<StatusDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    status: [this.data.hospital.status, [Validators.required]],
    reason: ['', [Validators.maxLength(255)]],
  });

  save(): void {
    if (this.form.invalid) return;
    const val = this.form.value;
    const req: UpdateHospitalStatusRequest = {
      status: val.status as HospitalStatus,
      reason: val.reason?.trim() || undefined,
    };
    this.dialogRef.close(req);
  }
}