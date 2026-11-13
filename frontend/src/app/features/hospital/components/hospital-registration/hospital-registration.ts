import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { NotificationService } from '../../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header';
import { HospitalRegistrationRequest, HospitalType } from '../../models/hospital.model';
import { HospitalService } from '../../services/hospital.service';

@Component({
  selector: 'app-hospital-registration',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatStepperModule,
    PageHeaderComponent,
  ],
  templateUrl: './hospital-registration.html',
  styleUrl: './hospital-registration.scss',
})
export class HospitalRegistrationComponent {
  private readonly fb = inject(FormBuilder);
  private readonly hospitalService = inject(HospitalService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  hidePassword = signal(true);

  readonly form: FormGroup = this.fb.group({
    // Step 1: Organization
    hospitalName: ['', [Validators.required, Validators.maxLength(150)]],
    hospitalCode: ['', [Validators.pattern(/^[A-Za-z0-9_-]{3,32}$/)]],
    registrationNumber: ['', [Validators.required, Validators.maxLength(64)]],
    hospitalType: ['GENERAL' as HospitalType, [Validators.required]],
    website: ['', [Validators.maxLength(255)]],
    establishedDate: [''],

    // Step 2: Location & Contact
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    primaryContactNumber: ['', [Validators.required, Validators.maxLength(32)]],
    alternateContactNumber: ['', [Validators.maxLength(32)]],
    addressLine1: ['', [Validators.required, Validators.maxLength(255)]],
    addressLine2: ['', [Validators.maxLength(255)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    district: ['', [Validators.required, Validators.maxLength(100)]],
    state: ['', [Validators.required, Validators.maxLength(100)]],
    country: ['India', [Validators.maxLength(100)]],
    pinCode: ['', [Validators.required, Validators.maxLength(20)]],

    // Step 3: Admin Account
    adminFirstName: ['', [Validators.required, Validators.maxLength(80)]],
    adminLastName: ['', [Validators.required, Validators.maxLength(80)]],
    adminUsername: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(64)]],
    adminEmail: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    adminPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+\-=\[\]{}|;:,.<>])[A-Za-z\d@$!%*?&#^()_+\-=\[\]{}|;:,.<>]{8,}$/),
      ],
    ],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.notificationService.warn('Please complete all required fields correctly.');
      return;
    }

    this.submitting.set(true);
    const val = this.form.value;

    const req: HospitalRegistrationRequest = {
      hospitalName: val.hospitalName.trim(),
      hospitalCode: val.hospitalCode ? val.hospitalCode.toUpperCase().trim() : undefined,
      registrationNumber: val.registrationNumber.trim(),
      hospitalType: val.hospitalType,
      website: val.website ? val.website.trim() : undefined,
      establishedDate: val.establishedDate || undefined,
      email: val.email.trim().toLowerCase(),
      primaryContactNumber: val.primaryContactNumber.trim(),
      alternateContactNumber: val.alternateContactNumber ? val.alternateContactNumber.trim() : undefined,
      addressLine1: val.addressLine1.trim(),
      addressLine2: val.addressLine2 ? val.addressLine2.trim() : undefined,
      city: val.city.trim(),
      district: val.district.trim(),
      state: val.state.trim(),
      country: val.country?.trim() || 'India',
      pinCode: val.pinCode.trim(),
      adminFirstName: val.adminFirstName.trim(),
      adminLastName: val.adminLastName.trim(),
      adminUsername: val.adminUsername.trim().toLowerCase(),
      adminEmail: val.adminEmail.trim().toLowerCase(),
      adminPassword: val.adminPassword,
    };

    this.hospitalService.registerHospital(req).subscribe({
      next: (hospital) => {
        this.submitting.set(false);
        this.notificationService.success(
          `Hospital "${hospital.name}" (${hospital.hospitalCode}) registered successfully.`
        );
        this.router.navigate(['/hospital']);
      },
      error: (err) => {
        this.submitting.set(false);
        this.notificationService.error(
          err.message || 'Hospital registration failed. Please check your inputs.'
        );
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/hospital']);
  }
}