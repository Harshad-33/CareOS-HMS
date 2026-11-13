import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

export interface DocumentUploadResult {
  file: File;
  documentType: string;
  expiryDate?: string;
}

@Component({
  selector: 'app-document-upload-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>Upload Compliance Document</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Document Type</mat-label>
          <mat-select formControlName="documentType">
            <mat-option value="REGISTRATION_CERTIFICATE">Hospital Registration Certificate</mat-option>
            <mat-option value="NABH_ACCREDITATION">NABH / Quality Accreditation</mat-option>
            <mat-option value="CLINICAL_ESTABLISHMENT_LICENSE">Clinical Establishment License</mat-option>
            <mat-option value="FIRE_SAFETY_NOC">Fire Safety NOC</mat-option>
            <mat-option value="POLLUTION_CONTROL_BOARD_NOC">Pollution Control Board NOC</mat-option>
            <mat-option value="TAX_ID">Tax / PAN / GST Registration</mat-option>
            <mat-option value="OTHER">Other Compliance Document</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Expiry Date (Optional)</mat-label>
          <input matInput type="date" formControlName="expiryDate" />
        </mat-form-field>

        <div class="file-dropzone" [class.has-file]="selectedFile() !== null">
          <input
            #fileInput
            type="file"
            (change)="onFileSelected($event)"
            accept=".pdf,.png,.jpg,.jpeg,.webp"
            style="display: none"
          />
          @if (selectedFile(); as file) {
            <div class="file-info">
              <mat-icon color="primary">description</mat-icon>
              <div class="file-meta">
                <span class="file-name">{{ file.name }}</span>
                <span class="file-size">{{ (file.size / 1024).toFixed(1) }} KB</span>
              </div>
              <button mat-icon-button color="warn" type="button" (click)="removeFile()">
                <mat-icon>close</mat-icon>
              </button>
            </div>
          } @else {
            <div class="dropzone-prompt" (click)="fileInput.click()">
              <mat-icon class="upload-icon">cloud_upload</mat-icon>
              <p>Click to browse and select document</p>
              <span class="hint">Supported: PDF, PNG, JPG, WEBP (Max 10MB)</span>
            </div>
          }
        </div>
        @if (fileError()) {
          <mat-error class="custom-error">{{ fileError() }}</mat-error>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || !selectedFile()" (click)="upload()">
        Upload Document
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
      min-width: 440px;
    }
    .full-width { width: 100%; }
    .file-dropzone {
      border: 2px dashed #cbd5e1;
      border-radius: 8px;
      padding: 20px;
      text-align: center;
      background: #f8fafc;
      transition: border-color 0.2s;
    }
    .file-dropzone.has-file {
      border-style: solid;
      border-color: #3b82f6;
      background: #eff6ff;
    }
    .dropzone-prompt {
      cursor: pointer;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
    }
    .upload-icon {
      font-size: 36px;
      height: 36px;
      width: 36px;
      color: #64748b;
    }
    .hint {
      font-size: 12px;
      color: #64748b;
    }
    .file-info {
      display: flex;
      align-items: center;
      gap: 12px;
      text-align: left;
    }
    .file-meta {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    .file-name {
      font-weight: 500;
      font-size: 14px;
    }
    .file-size {
      font-size: 12px;
      color: #64748b;
    }
    .custom-error {
      font-size: 12px;
      color: #dc2626;
    }
    `,
  ],
})
export class DocumentUploadDialogComponent {
  readonly dialogRef = inject(MatDialogRef<DocumentUploadDialogComponent>);
  private readonly fb = inject(FormBuilder);

  readonly selectedFile = signal<File | null>(null);
  readonly fileError = signal<string | null>(null);

  readonly form: FormGroup = this.fb.group({
    documentType: ['REGISTRATION_CERTIFICATE', [Validators.required]],
    expiryDate: [''],
  });

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.fileError.set(null);

    if (file.size > 10 * 1024 * 1024) {
      this.fileError.set('File size exceeds 10MB limit');
      this.selectedFile.set(null);
      return;
    }

    const validExtensions = ['pdf', 'png', 'jpg', 'jpeg', 'webp'];
    const ext = file.name.split('.').pop()?.toLowerCase();
    if (!ext || !validExtensions.includes(ext)) {
      this.fileError.set('Invalid file type. Only PDF, PNG, JPG, and WEBP are permitted.');
      this.selectedFile.set(null);
      return;
    }

    this.selectedFile.set(file);
  }

  removeFile(): void {
    this.selectedFile.set(null);
    this.fileError.set(null);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file || this.form.invalid) return;

    const res: DocumentUploadResult = {
      file,
      documentType: this.form.value.documentType,
      expiryDate: this.form.value.expiryDate || undefined,
    };
    this.dialogRef.close(res);
  }
}