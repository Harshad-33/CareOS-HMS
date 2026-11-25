import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { Department, HospitalBranch } from '../../../hospital/models/hospital.model';
import { HospitalService } from '../../../hospital/services/hospital.service';
import {
  Employee,
  EmployeeGender,
  EmployeeSummary,
  EmploymentType,
  UpdateEmployeeRequest,
} from '../../models/employee.model';
import { EmployeeService } from '../../services/employee.service';

export interface EmployeeEditDialogData {
  employee: Employee;
  focusSection?: 'personal' | 'professional';
}

@Component({
  selector: 'app-employee-edit-dialog',
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
    MatTabsModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="dialog-title-icon">edit</mat-icon>
      Edit Employee: {{ data.employee.fullName }} ({{ data.employee.employeeNumber }})
    </h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="edit-form">
        <mat-tab-group [selectedIndex]="selectedTabIndex()" (selectedIndexChange)="selectedTabIndex.set($event)">
          <!-- Tab 1: Personal Details -->
          <mat-tab label="Personal & Contact">
            <div class="tab-pane">
              <div class="grid-3">
                <mat-form-field appearance="outline">
                  <mat-label>First Name</mat-label>
                  <input matInput formControlName="firstName" />
                  @if (form.get('firstName')?.hasError('required')) {
                    <mat-error>First name is required</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Middle Name</mat-label>
                  <input matInput formControlName="middleName" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Last Name</mat-label>
                  <input matInput formControlName="lastName" />
                  @if (form.get('lastName')?.hasError('required')) {
                    <mat-error>Last name is required</mat-error>
                  }
                </mat-form-field>
              </div>

              <div class="grid-3">
                <mat-form-field appearance="outline">
                  <mat-label>Date of Birth</mat-label>
                  <input matInput type="date" formControlName="dateOfBirth" />
                  @if (form.get('dateOfBirth')?.hasError('required')) {
                    <mat-error>Date of birth is required</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Gender</mat-label>
                  <mat-select formControlName="gender">
                    <mat-option value="MALE">Male</mat-option>
                    <mat-option value="FEMALE">Female</mat-option>
                    <mat-option value="OTHER">Other</mat-option>
                  </mat-select>
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Blood Group</mat-label>
                  <mat-select formControlName="bloodGroup">
                    <mat-option [value]="''">Not Specified</mat-option>
                    <mat-option value="A_POSITIVE">A+</mat-option>
                    <mat-option value="A_NEGATIVE">A-</mat-option>
                    <mat-option value="B_POSITIVE">B+</mat-option>
                    <mat-option value="B_NEGATIVE">B-</mat-option>
                    <mat-option value="AB_POSITIVE">AB+</mat-option>
                    <mat-option value="AB_NEGATIVE">AB-</mat-option>
                    <mat-option value="O_POSITIVE">O+</mat-option>
                    <mat-option value="O_NEGATIVE">O-</mat-option>
                  </mat-select>
                </mat-form-field>
              </div>

              <h3>Contact Information</h3>
              <div class="grid-2">
                <mat-form-field appearance="outline">
                  <mat-label>Email Address</mat-label>
                  <input matInput type="email" formControlName="email" />
                  @if (form.get('email')?.hasError('required')) {
                    <mat-error>Email is required</mat-error>
                  }
                  @if (form.get('email')?.hasError('email')) {
                    <mat-error>Invalid email format</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Phone Number</mat-label>
                  <input matInput formControlName="phoneNumber" />
                  @if (form.get('phoneNumber')?.hasError('required')) {
                    <mat-error>Phone number is required</mat-error>
                  }
                  @if (form.get('phoneNumber')?.hasError('pattern')) {
                    <mat-error>Invalid phone format</mat-error>
                  }
                </mat-form-field>
              </div>

              <div class="grid-2">
                <mat-form-field appearance="outline">
                  <mat-label>Alternate Phone</mat-label>
                  <input matInput formControlName="alternatePhoneNumber" />
                </mat-form-field>
              </div>

              <h3>Residential Address</h3>
              <div class="grid-2">
                <mat-form-field appearance="outline">
                  <mat-label>Address Line 1</mat-label>
                  <input matInput formControlName="addressLine1" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Address Line 2</mat-label>
                  <input matInput formControlName="addressLine2" />
                </mat-form-field>
              </div>

              <div class="grid-4">
                <mat-form-field appearance="outline">
                  <mat-label>City</mat-label>
                  <input matInput formControlName="city" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>District</mat-label>
                  <input matInput formControlName="district" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>State</mat-label>
                  <input matInput formControlName="state" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>PIN Code</mat-label>
                  <input matInput formControlName="pinCode" />
                </mat-form-field>
              </div>

              <h3>Emergency Contact</h3>
              <div class="grid-3">
                <mat-form-field appearance="outline">
                  <mat-label>Contact Name</mat-label>
                  <input matInput formControlName="emergencyContactName" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Relationship</mat-label>
                  <input matInput formControlName="emergencyContactRelation" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Emergency Phone</mat-label>
                  <input matInput formControlName="emergencyContactPhone" />
                </mat-form-field>
              </div>
            </div>
          </mat-tab>

          <!-- Tab 2: Professional Assignment -->
          <mat-tab label="Professional Assignment">
            <div class="tab-pane">
              <div class="grid-2">
                <mat-form-field appearance="outline">
                  <mat-label>Designation</mat-label>
                  <input matInput formControlName="designation" />
                  @if (form.get('designation')?.hasError('required')) {
                    <mat-error>Designation is required</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Employment Type</mat-label>
                  <mat-select formControlName="employmentType">
                    <mat-option value="FULL_TIME">Full Time</mat-option>
                    <mat-option value="PART_TIME">Part Time</mat-option>
                    <mat-option value="CONTRACT">Contract</mat-option>
                    <mat-option value="PROBATION">Probation</mat-option>
                    <mat-option value="INTERN">Intern</mat-option>
                    <mat-option value="VISITING_CONSULTANT">Visiting Consultant</mat-option>
                  </mat-select>
                </mat-form-field>
              </div>

              <div class="grid-2">
                <mat-form-field appearance="outline">
                  <mat-label>Department</mat-label>
                  <mat-select formControlName="departmentPublicId">
                    @for (dept of departments(); track dept.publicId) {
                      <mat-option [value]="dept.publicId">{{ dept.departmentName }} ({{ dept.departmentCode }})</mat-option>
                    }
                  </mat-select>
                  @if (form.get('departmentPublicId')?.hasError('required')) {
                    <mat-error>Department is required</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Branch</mat-label>
                  <mat-select formControlName="branchPublicId">
                    <mat-option [value]="''">Primary / Hospital Headquarters</mat-option>
                    @for (br of branches(); track br.publicId) {
                      <mat-option [value]="br.publicId">{{ br.branchName }} ({{ br.branchCode }})</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              </div>

              <div class="grid-2">
                <mat-form-field appearance="outline">
                  <mat-label>Date of Joining</mat-label>
                  <input matInput type="date" formControlName="joiningDate" />
                  @if (form.get('joiningDate')?.hasError('required')) {
                    <mat-error>Joining date is required</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Date of Leaving (Optional)</mat-label>
                  <input matInput type="date" formControlName="leavingDate" />
                </mat-form-field>
              </div>

              <div class="grid-2">
                <mat-form-field appearance="outline">
                  <mat-label>Reporting Manager (Optional)</mat-label>
                  <mat-select formControlName="reportingManagerPublicId">
                    <mat-option [value]="''">None</mat-option>
                    @for (mgr of managerOptions(); track mgr.publicId) {
                      <mat-option [value]="mgr.publicId">{{ mgr.fullName }} ({{ mgr.designation }})</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              </div>
            </div>
          </mat-tab>
        </mat-tab-group>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">
        Save Changes
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-title-icon {
        vertical-align: middle;
        margin-right: 8px;
        color: #2563eb;
      }
      .edit-form {
        min-width: 650px;
        max-width: 800px;
        padding-top: 8px;
      }
      .tab-pane {
        padding: 16px 4px 8px 4px;
        display: flex;
        flex-direction: column;
        gap: 12px;

        h3 {
          font-size: 14px;
          font-weight: 600;
          color: #475569;
          margin: 8px 0 0 0;
          padding-bottom: 4px;
          border-bottom: 1px solid #e2e8f0;
        }
      }
      .grid-2 {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
      }
      .grid-3 {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 12px;
      }
      .grid-4 {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 12px;
      }
      @media (max-width: 700px) {
        .edit-form {
          min-width: 100%;
        }
        .grid-2,
        .grid-3,
        .grid-4 {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class EmployeeEditDialogComponent implements OnInit {
  readonly data = inject<EmployeeEditDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EmployeeEditDialogComponent>);
  private readonly fb = inject(FormBuilder);
  private readonly hospitalService = inject(HospitalService);
  private readonly employeeService = inject(EmployeeService);

  readonly departments = signal<Department[]>([]);
  readonly branches = signal<HospitalBranch[]>([]);
  readonly managerOptions = signal<EmployeeSummary[]>([]);
  readonly selectedTabIndex = signal<number>(
    this.data.focusSection === 'professional' ? 1 : 0
  );

  readonly form: FormGroup = this.fb.group({
    firstName: [this.data.employee.firstName, [Validators.required, Validators.maxLength(80)]],
    middleName: [this.data.employee.middleName || '', [Validators.maxLength(80)]],
    lastName: [this.data.employee.lastName, [Validators.required, Validators.maxLength(80)]],
    dateOfBirth: [this.data.employee.dateOfBirth, [Validators.required]],
    gender: [this.data.employee.gender as EmployeeGender, [Validators.required]],
    bloodGroup: [this.data.employee.bloodGroup || ''],
    email: [
      this.data.employee.email,
      [Validators.required, Validators.email, Validators.maxLength(160)],
    ],
    phoneNumber: [
      this.data.employee.phoneNumber,
      [Validators.required, Validators.pattern(/^[0-9+()\-\s]{7,20}$/)],
    ],
    alternatePhoneNumber: [
      this.data.employee.alternatePhoneNumber || '',
      [Validators.pattern(/^[0-9+()\-\s]{7,20}$/)],
    ],
    addressLine1: [this.data.employee.addressLine1 || '', [Validators.maxLength(255)]],
    addressLine2: [this.data.employee.addressLine2 || '', [Validators.maxLength(255)]],
    city: [this.data.employee.city || '', [Validators.maxLength(100)]],
    district: [this.data.employee.district || '', [Validators.maxLength(100)]],
    state: [this.data.employee.state || '', [Validators.maxLength(100)]],
    pinCode: [this.data.employee.pinCode || '', [Validators.maxLength(20)]],
    emergencyContactName: [
      this.data.employee.emergencyContactName || '',
      [Validators.maxLength(100)],
    ],
    emergencyContactRelation: [
      this.data.employee.emergencyContactRelation || '',
      [Validators.maxLength(50)],
    ],
    emergencyContactPhone: [
      this.data.employee.emergencyContactPhone || '',
      [Validators.pattern(/^[0-9+()\-\s]{7,20}$/)],
    ],
    designation: [
      this.data.employee.designation,
      [Validators.required, Validators.maxLength(100)],
    ],
    employmentType: [
      this.data.employee.employmentType as EmploymentType,
      [Validators.required],
    ],
    departmentPublicId: [this.data.employee.departmentPublicId || '', [Validators.required]],
    branchPublicId: [this.data.employee.branchPublicId || ''],
    joiningDate: [this.data.employee.joiningDate, [Validators.required]],
    leavingDate: [this.data.employee.leavingDate || ''],
    reportingManagerPublicId: [this.data.employee.reportingManagerPublicId || ''],
  });

  ngOnInit(): void {
    this.hospitalService.getMyDepartments().subscribe({
      next: (depts) => this.departments.set(depts.filter((d) => d.status === 'ACTIVE')),
    });
    this.hospitalService.getMyBranches().subscribe({
      next: (branches) => this.branches.set(branches.filter((b) => b.status === 'ACTIVE')),
    });
    this.employeeService.getEmployees({ size: 100, status: 'ACTIVE' }).subscribe({
      next: (page) =>
        this.managerOptions.set(
          page.content.filter((m) => m.publicId !== this.data.employee.publicId)
        ),
    });
  }

  save(): void {
    if (this.form.invalid) return;

    const val = this.form.value;
    const request: UpdateEmployeeRequest = {
      firstName: val.firstName.trim(),
      middleName: val.middleName ? val.middleName.trim() : undefined,
      lastName: val.lastName.trim(),
      dateOfBirth: val.dateOfBirth,
      gender: val.gender,
      bloodGroup: val.bloodGroup || undefined,
      email: val.email.trim(),
      phoneNumber: val.phoneNumber.trim(),
      alternatePhoneNumber: val.alternatePhoneNumber ? val.alternatePhoneNumber.trim() : undefined,
      addressLine1: val.addressLine1 ? val.addressLine1.trim() : undefined,
      addressLine2: val.addressLine2 ? val.addressLine2.trim() : undefined,
      city: val.city ? val.city.trim() : undefined,
      district: val.district ? val.district.trim() : undefined,
      state: val.state ? val.state.trim() : undefined,
      country: undefined,
      pinCode: val.pinCode ? val.pinCode.trim() : undefined,
      emergencyContactName: val.emergencyContactName ? val.emergencyContactName.trim() : undefined,
      emergencyContactRelation: val.emergencyContactRelation
        ? val.emergencyContactRelation.trim()
        : undefined,
      emergencyContactPhone: val.emergencyContactPhone ? val.emergencyContactPhone.trim() : undefined,
      designation: val.designation.trim(),
      departmentPublicId: val.departmentPublicId,
      branchPublicId: val.branchPublicId || undefined,
      employmentType: val.employmentType,
      joiningDate: val.joiningDate,
      leavingDate: val.leavingDate || undefined,
      reportingManagerPublicId: val.reportingManagerPublicId || undefined,
    };

    this.dialogRef.close(request);
  }
}
