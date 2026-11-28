import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { RejectLeaveStepRequest } from '../../models/leave.models';

export interface RejectReasonDialogData {
  applicationLabel: string;
  stepNumber: number;
}

@Component({
  selector: 'app-reject-reason-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <mat-icon color="warn">warning</mat-icon>
        <h2 mat-dialog-title>Reject Leave Application</h2>
      </div>

      <p class="subtitle">
        You are rejecting Step {{ data.stepNumber }} of application <strong>{{ data.applicationLabel }}</strong>.
        A rejection reason is mandatory.
      </p>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-dialog-content>
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Rejection Reason (Mandatory)</mat-label>
            <textarea
              matInput
              rows="3"
              formControlName="rejectionReason"
              placeholder="Explain why this leave application is being rejected..."
            ></textarea>
            <mat-error *ngIf="form.get('rejectionReason')?.hasError('required')">
              Rejection reason is required
            </mat-error>
            <mat-error *ngIf="form.get('rejectionReason')?.hasError('maxlength')">
              Reason cannot exceed 255 characters
            </mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Internal Comments (Optional)</mat-label>
            <input matInput formControlName="comments" placeholder="Additional notes for HR/management..." />
            <mat-error *ngIf="form.get('comments')?.hasError('maxlength')">
              Comments cannot exceed 255 characters
            </mat-error>
          </mat-form-field>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
          <button mat-button type="button" mat-dialog-close>Cancel</button>
          <button mat-flat-button color="warn" type="submit" [disabled]="form.invalid">
            Confirm Rejection
          </button>
        </mat-dialog-actions>
      </form>
    </div>
  `,
  styles: [`
    .dialog-container { padding: 8px 16px; min-width: 420px; }
    .dialog-header { display: flex; align-items: center; gap: 8px; }
    .dialog-header h2 { margin: 0; font-size: 1.2rem; }
    .subtitle { font-size: 0.9rem; color: #616161; margin: 8px 0 16px 0; }
    .w-full { width: 100%; margin-bottom: 8px; }
  `],
})
export class RejectReasonDialogComponent {
  readonly data: RejectReasonDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<RejectReasonDialogComponent, RejectLeaveStepRequest>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    rejectionReason: ['', [Validators.required, Validators.maxLength(255)]],
    comments: ['', [Validators.maxLength(255)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    const value = this.form.value;
    this.dialogRef.close({
      rejectionReason: value.rejectionReason!.trim(),
      comments: value.comments ? value.comments.trim() : undefined,
    });
  }
}
