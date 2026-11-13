import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { HospitalDocument, VerifyDocumentRequest } from '../../models/hospital.model';

export interface VerifyDocumentDialogData {
  document: HospitalDocument;
}

@Component({
  selector: 'app-verify-document-dialog',
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
    <h2 mat-dialog-title>Review Document: {{ data.document.documentName }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <p class="doc-meta">
          <strong>Type:</strong> {{ data.document.documentType }} |
          <strong>Size:</strong> {{ (data.document.fileSizeBytes / 1024).toFixed(1) }} KB
        </p>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Verification Decision</mat-label>
          <mat-select formControlName="verificationStatus">
            <mat-option value="VERIFIED">VERIFIED (Approve)</mat-option>
            <mat-option value="REJECTED">REJECTED (Reject)</mat-option>
          </mat-select>
        </mat-form-field>

        @if (form.get('verificationStatus')?.value === 'REJECTED') {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Rejection Reason</mat-label>
            <textarea
              matInput
              rows="3"
              formControlName="rejectionReason"
              placeholder="Explain why this document was rejected (e.g. illegible scan, expired certificate)"
            ></textarea>
            @if (form.get('rejectionReason')?.hasError('required')) {
              <mat-error>Rejection reason is required when rejecting</mat-error>
            }
          </mat-form-field>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">
        Submit Decision
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
      min-width: 400px;
    }
    .full-width { width: 100%; }
    .doc-meta { font-size: 13px; color: #475569; margin-bottom: 8px; }
    `,
  ],
})
export class VerifyDocumentDialogComponent {
  readonly dialogRef = inject(MatDialogRef<VerifyDocumentDialogComponent>);
  readonly data = inject<VerifyDocumentDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    verificationStatus: ['VERIFIED', [Validators.required]],
    rejectionReason: [''],
  });

  save(): void {
    const val = this.form.value;
    if (val.verificationStatus === 'REJECTED' && (!val.rejectionReason || !val.rejectionReason.trim())) {
      this.form.get('rejectionReason')?.setErrors({ required: true });
      return;
    }

    const req: VerifyDocumentRequest = {
      verificationStatus: val.verificationStatus,
      rejectionReason: val.verificationStatus === 'REJECTED' ? val.rejectionReason.trim() : undefined,
    };
    this.dialogRef.close(req);
  }
}