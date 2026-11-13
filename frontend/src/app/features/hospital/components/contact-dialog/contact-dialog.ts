import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { HospitalContact, HospitalContactRequest } from '../../models/hospital.model';

export interface ContactDialogData {
  contact?: HospitalContact;
  title: string;
}

@Component({
  selector: 'app-contact-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Contact Type</mat-label>
          <mat-select formControlName="contactType">
            <mat-option value="GENERAL">General Support</mat-option>
            <mat-option value="EMERGENCY">Emergency Response</mat-option>
            <mat-option value="BILLING">Billing & Accounts</mat-option>
            <mat-option value="ADMINISTRATIVE">Administrative</mat-option>
            <mat-option value="TECHNICAL">Technical / IT</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Full Name</mat-label>
          <input matInput formControlName="name" placeholder="Contact person name" />
          @if (form.get('name')?.hasError('required')) {
            <mat-error>Name is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Designation</mat-label>
          <input matInput formControlName="designation" placeholder="e.g. Chief Medical Officer, IT Admin" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Phone Number</mat-label>
          <input matInput formControlName="phoneNumber" placeholder="+91..." />
          @if (form.get('phoneNumber')?.hasError('required')) {
            <mat-error>Phone number is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Email Address</mat-label>
          <input matInput type="email" formControlName="email" placeholder="contact@hospital.com" />
        </mat-form-field>

        <mat-checkbox formControlName="primary" color="primary">
          Mark as Primary Hospital Contact
        </mat-checkbox>
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
      min-width: 400px;
    }
    .full-width { width: 100%; }
    `,
  ],
})
export class ContactDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ContactDialogComponent>);
  readonly data = inject<ContactDialogData>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    contactType: [this.data.contact?.contactType ?? 'GENERAL', [Validators.required]],
    name: [this.data.contact?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    designation: [this.data.contact?.designation ?? '', [Validators.maxLength(100)]],
    phoneNumber: [this.data.contact?.phoneNumber ?? '', [Validators.required, Validators.maxLength(32)]],
    email: [this.data.contact?.email ?? '', [Validators.email, Validators.maxLength(160)]],
    primary: [this.data.contact?.primary ?? false],
  });

  save(): void {
    if (this.form.invalid) return;
    const val = this.form.value;
    const req: HospitalContactRequest = {
      contactType: val.contactType,
      name: val.name.trim(),
      designation: val.designation?.trim(),
      phoneNumber: val.phoneNumber.trim(),
      email: val.email?.trim(),
      primary: val.primary,
    };
    this.dialogRef.close(req);
  }
}