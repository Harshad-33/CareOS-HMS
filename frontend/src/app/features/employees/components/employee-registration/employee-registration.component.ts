import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header';
import { Department, HospitalBranch } from '../../../hospital/models/hospital.model';
import { HospitalService } from '../../../hospital/services/hospital.service';
import {
  CreateEmployeeRequest,
  EmployeeGender,
  EmployeeSummary,
  EmploymentType,
} from '../../models/employee.model';
import { EmployeeService } from '../../services/employee.service';

@Component({
  selector: 'app-employee-registration',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatStepperModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
  ],
  templateUrl: './employee-registration.component.html',
  styleUrls: ['./employee-registration.component.scss'],
})
export class EmployeeRegistrationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);
  private readonly hospitalService = inject(HospitalService);
  private readonly authStateService = inject(AuthStateService);
  private readonly notificationService = inject(NotificationService);

  readonly departments = signal<Department[]>([]);
  readonly branches = signal<HospitalBranch[]>([]);
  readonly managerOptions = signal<EmployeeSummary[]>([]);
  readonly photoPreviewUrl = signal<string | null>(null);
  readonly selectedPhotoFile = signal<File | null>(null);
  readonly submitting = signal<boolean>(false);
  readonly hidePassword = signal<boolean>(true);

  readonly canLinkAccount = computed(() =>
    this.authStateService.hasPermission('HOSPITAL_EMPLOYEES_ACCOUNT_LINK')
  );

  readonly form: FormGroup = this.fb.group({
    personal: this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(80)]],
      middleName: ['', [Validators.maxLength(80)]],
      lastName: ['', [Validators.required, Validators.maxLength(80)]],
      dateOfBirth: ['', [Validators.required]],
      gender: ['MALE' as EmployeeGender, [Validators.required]],
      bloodGroup: [''],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
      phoneNumber: [
        '',
        [Validators.required, Validators.pattern(/^[0-9+()\-\s]{7,20}$/)],
      ],
      alternatePhoneNumber: ['', [Validators.pattern(/^[0-9+()\-\s]{7,20}$/)]],
      addressLine1: ['', [Validators.maxLength(255)]],
      addressLine2: ['', [Validators.maxLength(255)]],
      city: ['', [Validators.maxLength(100)]],
      district: ['', [Validators.maxLength(100)]],
      state: ['', [Validators.maxLength(100)]],
      pinCode: ['', [Validators.maxLength(20)]],
      emergencyContactName: ['', [Validators.maxLength(100)]],
      emergencyContactRelation: ['', [Validators.maxLength(50)]],
      emergencyContactPhone: ['', [Validators.pattern(/^[0-9+()\-\s]{7,20}$/)]],
    }),
    professional: this.fb.group({
      autoGenerateCode: [true],
      employeeNumber: [''],
      designation: ['', [Validators.required, Validators.maxLength(100)]],
      employmentType: ['FULL_TIME' as EmploymentType, [Validators.required]],
      departmentPublicId: ['', [Validators.required]],
      branchPublicId: [''],
      joiningDate: ['', [Validators.required]],
      reportingManagerPublicId: [''],
    }),
    account: this.fb.group({
      createUserAccount: [false],
      username: [''],
      password: [''],
      roleCodes: [['HOSPITAL_ADMIN']],
    }),
  });

  get personalGroup(): FormGroup {
    return this.form.get('personal') as FormGroup;
  }

  get professionalGroup(): FormGroup {
    return this.form.get('professional') as FormGroup;
  }

  get accountGroup(): FormGroup {
    return this.form.get('account') as FormGroup;
  }

  ngOnInit(): void {
    this.loadDepartments();
    this.loadBranches();
    this.loadManagers();
  }

  loadDepartments(): void {
    this.hospitalService.getMyDepartments().subscribe({
      next: (depts) => {
        this.departments.set(depts.filter((d) => d.status === 'ACTIVE'));
      },
      error: () => {
        this.notificationService.error('Failed to load departments');
      },
    });
  }

  loadBranches(): void {
    this.hospitalService.getMyBranches().subscribe({
      next: (branches) => {
        this.branches.set(branches.filter((b) => b.status === 'ACTIVE'));
      },
      error: () => {
        this.notificationService.error('Failed to load branches');
      },
    });
  }

  loadManagers(): void {
    this.employeeService.getEmployees({ size: 100, status: 'ACTIVE' }).subscribe({
      next: (page) => {
        this.managerOptions.set(page.content);
      },
      error: () => {
        // Non-critical, ignore or log silently
      },
    });
  }

  onAutoGenerateCodeToggle(checked: boolean): void {
    const codeControl = this.professionalGroup.get('employeeNumber');
    if (checked) {
      codeControl?.clearValidators();
      codeControl?.setValue('');
    } else {
      codeControl?.setValidators([Validators.required, Validators.maxLength(32)]);
    }
    codeControl?.updateValueAndValidity();
  }

  onAccountToggle(checked: boolean): void {
    const usernameCtrl = this.accountGroup.get('username');
    const passwordCtrl = this.accountGroup.get('password');

    if (checked && this.canLinkAccount()) {
      usernameCtrl?.setValidators([
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(80),
      ]);
      passwordCtrl?.setValidators([
        Validators.required,
        Validators.minLength(12),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d\s]).{12,}$/),
      ]);
    } else {
      usernameCtrl?.clearValidators();
      passwordCtrl?.clearValidators();
      usernameCtrl?.setValue('');
      passwordCtrl?.setValue('');
    }
    usernameCtrl?.updateValueAndValidity();
    passwordCtrl?.updateValueAndValidity();
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    if (file.size > 10 * 1024 * 1024) {
      this.notificationService.error('Profile photo must be less than 10MB');
      return;
    }

    this.selectedPhotoFile.set(file);
    const reader = new FileReader();
    reader.onload = () => {
      this.photoPreviewUrl.set(reader.result as string);
    };
    reader.readAsDataURL(file);
  }

  removePhoto(): void {
    this.selectedPhotoFile.set(null);
    this.photoPreviewUrl.set(null);
  }

  getFullName(): string {
    const p = this.personalGroup.value;
    return [p.firstName, p.middleName, p.lastName].filter(Boolean).join(' ');
  }

  getSelectedDepartmentName(): string {
    const deptId = this.professionalGroup.get('departmentPublicId')?.value;
    const dept = this.departments().find((d) => d.publicId === deptId);
    return dept ? `${dept.departmentName} (${dept.departmentCode})` : 'None';
  }

  cancel(): void {
    this.router.navigate(['/employees']);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.notificationService.error('Please resolve all validation errors before submitting.');
      return;
    }

    this.submitting.set(true);

    const pVal = this.personalGroup.value;
    const profVal = this.professionalGroup.value;
    const accVal = this.accountGroup.value;

    const request: CreateEmployeeRequest = {
      firstName: pVal.firstName.trim(),
      middleName: pVal.middleName ? pVal.middleName.trim() : undefined,
      lastName: pVal.lastName.trim(),
      dateOfBirth: pVal.dateOfBirth,
      gender: pVal.gender,
      bloodGroup: pVal.bloodGroup || undefined,
      email: pVal.email.trim(),
      phoneNumber: pVal.phoneNumber.trim(),
      alternatePhoneNumber: pVal.alternatePhoneNumber ? pVal.alternatePhoneNumber.trim() : undefined,
      addressLine1: pVal.addressLine1 ? pVal.addressLine1.trim() : undefined,
      addressLine2: pVal.addressLine2 ? pVal.addressLine2.trim() : undefined,
      city: pVal.city ? pVal.city.trim() : undefined,
      district: pVal.district ? pVal.district.trim() : undefined,
      state: pVal.state ? pVal.state.trim() : undefined,
      pinCode: pVal.pinCode ? pVal.pinCode.trim() : undefined,
      emergencyContactName: pVal.emergencyContactName ? pVal.emergencyContactName.trim() : undefined,
      emergencyContactRelation: pVal.emergencyContactRelation ? pVal.emergencyContactRelation.trim() : undefined,
      emergencyContactPhone: pVal.emergencyContactPhone ? pVal.emergencyContactPhone.trim() : undefined,
      designation: profVal.designation.trim(),
      departmentPublicId: profVal.departmentPublicId,
      branchPublicId: profVal.branchPublicId || undefined,
      employmentType: profVal.employmentType,
      joiningDate: profVal.joiningDate,
      employeeNumber: !profVal.autoGenerateCode && profVal.employeeNumber ? profVal.employeeNumber.trim() : undefined,
      reportingManagerPublicId: profVal.reportingManagerPublicId || undefined,
      createUserAccount: this.canLinkAccount() && accVal.createUserAccount ? true : false,
      username: this.canLinkAccount() && accVal.createUserAccount ? accVal.username.trim() : undefined,
      password: this.canLinkAccount() && accVal.createUserAccount ? accVal.password : undefined,
      roleCodes: this.canLinkAccount() && accVal.createUserAccount ? accVal.roleCodes : undefined,
    };

    this.employeeService.createEmployee(request).subscribe({
      next: (created) => {
        const photoFile = this.selectedPhotoFile();
        if (photoFile) {
          this.employeeService.uploadProfilePhoto(created.publicId, photoFile).subscribe({
            next: () => {
              this.submitting.set(false);
              this.notificationService.success('Employee registered and photo uploaded successfully.');
              this.router.navigate(['/employees']);
            },
            error: () => {
              this.submitting.set(false);
              this.notificationService.warn(
                'Employee registered successfully, but photo upload encountered an issue.'
              );
              this.router.navigate(['/employees']);
            },
          });
        } else {
          this.submitting.set(false);
          this.notificationService.success('Employee registered successfully.');
          this.router.navigate(['/employees']);
        }
      },
      error: (err) => {
        this.submitting.set(false);
        const errorMsg = err?.error?.message || 'Failed to register employee. Please check your inputs.';
        this.notificationService.error(errorMsg);
      },
    });
  }
}
