import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthStateService } from '../../core/services/auth-state.service';
import { NotificationService } from '../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header';
import { ConfirmDialogData } from '../../shared/models/confirm-dialog.model';
import { BranchDialogComponent } from './components/branch-dialog/branch-dialog';
import { ContactDialogComponent } from './components/contact-dialog/contact-dialog';
import { DepartmentDialogComponent } from './components/department-dialog/department-dialog';
import { DocumentUploadDialogComponent, DocumentUploadResult } from './components/document-upload-dialog/document-upload-dialog';
import { StatusDialogComponent } from './components/status-dialog/status-dialog';
import { VerifyDocumentDialogComponent } from './components/verify-document-dialog/verify-document-dialog';
import {
  AuditLog,
  Department,
  Hospital,
  HospitalBranch,
  HospitalContact,
  HospitalDocument,
  HospitalSettings,
  HospitalStatus,
  HospitalType,
  UpdateHospitalProfileRequest,
  VerifyDocumentRequest,
} from './models/hospital.model';
import { HospitalService } from './services/hospital.service';

@Component({
  selector: 'app-hospital',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSlideToggleModule,
    MatTooltipModule,
    PageHeaderComponent,
    LoadingIndicatorComponent,
    EmptyStateComponent,
  ],
  templateUrl: './hospital.component.html',
  styleUrl: './hospital.component.scss',
})
export class HospitalComponent implements OnInit {
  private readonly hospitalService = inject(HospitalService);
  private readonly authState = inject(AuthStateService);
  private readonly dialog = inject(MatDialog);
  private readonly notificationService = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  readonly isPlatformAdmin = computed(() => this.authState.isPlatformUser());
  readonly isHospitalAdmin = computed(() => this.authState.isHospitalUser());
  readonly loading = signal(false);

  readonly hospitals = signal<Hospital[]>([]);
  readonly totalHospitals = signal(0);
  readonly hospitalFilterStatus = signal<HospitalStatus | undefined>(undefined);
  readonly hospitalSearchQuery = signal<string>('');

  readonly myHospital = signal<Hospital | null>(null);
  readonly mySettings = signal<HospitalSettings | null>(null);
  readonly departments = signal<Department[]>([]);
  readonly branches = signal<HospitalBranch[]>([]);
  readonly contacts = signal<HospitalContact[]>([]);
  readonly documents = signal<HospitalDocument[]>([]);
  readonly auditLogs = signal<AuditLog[]>([]);

  profileForm!: FormGroup;
  settingsForm!: FormGroup;

  ngOnInit(): void {
    this.initForms();
    if (this.isPlatformAdmin()) {
      this.loadPlatformHospitals();
    } else {
      this.loadHospitalAdminData();
    }
  }

  private initForms(): void {
    this.profileForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(150)]],
      hospitalType: ['GENERAL' as HospitalType, [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      primaryContactNumber: ['', [Validators.required]],
      alternateContactNumber: [''],
      website: [''],
      addressLine1: ['', [Validators.required]],
      addressLine2: [''],
      city: ['', [Validators.required]],
      district: ['', [Validators.required]],
      state: ['', [Validators.required]],
      country: ['India'],
      pinCode: ['', [Validators.required]],
      establishedDate: [''],
    });

    this.settingsForm = this.fb.group({
      timezone: ['Asia/Kolkata', [Validators.required]],
      currency: ['INR', [Validators.required]],
      dateFormat: ['YYYY-MM-DD', [Validators.required]],
      timeFormat: ['24_HOUR', [Validators.required]],
      workingDays: ['MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY', [Validators.required]],
      workingHoursStart: ['09:00:00', [Validators.required]],
      workingHoursEnd: ['18:00:00', [Validators.required]],
      appointmentSlotDurationMinutes: [15, [Validators.required, Validators.min(5), Validators.max(120)]],
      maxAdvanceBookingDays: [30, [Validators.required, Validators.min(1), Validators.max(365)]],
      emergencyContactNumber: [''],
      supportEmail: ['', [Validators.email]],
    });
  }

  loadPlatformHospitals(): void {
    this.loading.set(true);
    this.hospitalService
      .getHospitals(this.hospitalFilterStatus(), this.hospitalSearchQuery())
      .subscribe({
        next: (page) => {
          this.hospitals.set(page.content);
          this.totalHospitals.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.notificationService.error(err.message || 'Failed to load hospitals');
          this.loading.set(false);
        },
      });
  }

  onFilterStatusChange(status: string): void {
    this.hospitalFilterStatus.set(status === 'ALL' ? undefined : (status as HospitalStatus));
    this.loadPlatformHospitals();
  }

  onSearch(query: string): void {
    this.hospitalSearchQuery.set(query.trim());
    this.loadPlatformHospitals();
  }

  openStatusDialog(hospital: Hospital): void {
    this.dialog
      .open(StatusDialogComponent, { data: { hospital }, width: '450px' })
      .afterClosed()
      .subscribe((req) => {
        if (req) {
          this.hospitalService.updateHospitalStatus(hospital.publicId, req).subscribe({
            next: (updated) => {
              this.notificationService.success(
                `Status for "${updated.name}" updated to ${updated.status}`
              );
              this.loadPlatformHospitals();
            },
            error: (err) => this.notificationService.error(err.message || 'Status update failed'),
          });
        }
      });
  }

  loadHospitalAdminData(): void {
    this.loading.set(true);
    this.hospitalService.getMyHospital().subscribe({
      next: (hosp) => {
        this.myHospital.set(hosp);
        this.populateProfileForm(hosp);
        this.loadSettings();
        this.loadDepartments();
        this.loadBranches();
        this.loadContacts();
        this.loadDocuments();
        this.loadAudits();
        this.loading.set(false);
      },
      error: (err) => {
        this.notificationService.error(err.message || 'Failed to load hospital profile');
        this.loading.set(false);
      },
    });
  }

  private populateProfileForm(hosp: Hospital): void {
    this.profileForm.patchValue({
      name: hosp.name,
      hospitalType: hosp.hospitalType,
      email: hosp.email ?? '',
      primaryContactNumber: hosp.primaryContactNumber ?? '',
      alternateContactNumber: hosp.alternateContactNumber ?? '',
      website: hosp.website ?? '',
      addressLine1: hosp.addressLine1 ?? '',
      addressLine2: hosp.addressLine2 ?? '',
      city: hosp.city ?? '',
      district: hosp.district ?? '',
      state: hosp.state ?? '',
      country: hosp.country ?? 'India',
      pinCode: hosp.pinCode ?? '',
      establishedDate: hosp.establishedDate ?? '',
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.notificationService.warn('Please fill all required profile fields correctly.');
      return;
    }
    this.hospitalService.updateMyHospital(this.profileForm.value).subscribe({
      next: (updated) => {
        this.myHospital.set(updated);
        this.notificationService.success('Hospital profile updated successfully.');
      },
      error: (err) => this.notificationService.error(err.message || 'Profile update failed'),
    });
  }

  loadSettings(): void {
    this.hospitalService.getMySettings().subscribe({
      next: (s) => {
        this.mySettings.set(s);
        this.settingsForm.patchValue({
          timezone: s.timezone,
          currency: s.currency,
          dateFormat: s.dateFormat,
          timeFormat: s.timeFormat,
          workingDays: s.workingDays,
          workingHoursStart: s.workingHoursStart,
          workingHoursEnd: s.workingHoursEnd,
          appointmentSlotDurationMinutes: s.appointmentSlotDurationMinutes,
          maxAdvanceBookingDays: s.maxAdvanceBookingDays,
          emergencyContactNumber: s.emergencyContactNumber ?? '',
          supportEmail: s.supportEmail ?? '',
        });
      },
    });
  }

  saveSettings(): void {
    if (this.settingsForm.invalid) {
      this.notificationService.warn('Please fill all settings fields correctly.');
      return;
    }
    this.hospitalService.updateMySettings(this.settingsForm.value).subscribe({
      next: (updated) => {
        this.mySettings.set(updated);
        this.notificationService.success('Operational settings saved successfully.');
      },
      error: (err) => this.notificationService.error(err.message || 'Settings update failed'),
    });
  }

  loadDepartments(): void {
    this.hospitalService.getMyDepartments().subscribe({
      next: (list) => this.departments.set(list),
    });
  }

  openAddDepartmentDialog(): void {
    this.dialog
      .open(DepartmentDialogComponent, {
        data: { title: 'Add New Department' },
        width: '450px',
      })
      .afterClosed()
      .subscribe((req) => {
        if (req) {
          this.hospitalService.createMyDepartment(req).subscribe({
            next: () => {
              this.notificationService.success('Department created successfully.');
              this.loadDepartments();
            },
            error: (err) => this.notificationService.error(err.message || 'Failed to create department'),
          });
        }
      });
  }

  openEditDepartmentDialog(dept: Department): void {
    this.dialog
      .open(DepartmentDialogComponent, {
        data: { department: dept, title: 'Edit Department' },
        width: '450px',
      })
      .afterClosed()
      .subscribe((req) => {
        if (req) {
          this.hospitalService.updateMyDepartment(dept.publicId, req).subscribe({
            next: () => {
              this.notificationService.success('Department updated successfully.');
              this.loadDepartments();
            },
            error: (err) => this.notificationService.error(err.message || 'Failed to update department'),
          });
        }
      });
  }

  toggleDepartmentStatus(dept: Department): void {
    this.hospitalService.toggleDepartmentStatus(dept.publicId).subscribe({
      next: (updated) => {
        this.notificationService.info(`Department "${updated.departmentName}" status: ${updated.status}`);
        this.loadDepartments();
      },
      error: (err) => this.notificationService.error(err.message || 'Status toggle failed'),
    });
  }

  confirmDeleteDepartment(dept: Department): void {
    const data: ConfirmDialogData = {
      title: 'Delete Department',
      message: `Delete "${dept.departmentName}" (${dept.departmentCode})?`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'warn',
    };
    this.dialog
      .open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.hospitalService.deleteMyDepartment(dept.publicId).subscribe({
            next: () => {
              this.notificationService.success('Department deleted successfully.');
              this.loadDepartments();
            },
            error: (err) => this.notificationService.error(err.message || 'Delete failed'),
          });
        }
      });
  }

  loadBranches(): void {
    this.hospitalService.getMyBranches().subscribe({
      next: (list) => this.branches.set(list),
    });
  }

  openAddBranchDialog(): void {
    this.dialog
      .open(BranchDialogComponent, {
        data: { title: 'Add New Hospital Branch' },
        width: '520px',
      })
      .afterClosed()
      .subscribe((req) => {
        if (req) {
          this.hospitalService.createMyBranch(req).subscribe({
            next: () => {
              this.notificationService.success('Branch added successfully.');
              this.loadBranches();
            },
            error: (err) => this.notificationService.error(err.message || 'Failed to add branch'),
          });
        }
      });
  }

  openEditBranchDialog(branch: HospitalBranch): void {
    this.dialog
      .open(BranchDialogComponent, {
        data: { branch, title: 'Edit Branch' },
        width: '520px',
      })
      .afterClosed()
      .subscribe((req) => {
        if (req) {
          this.hospitalService.updateMyBranch(branch.publicId, req).subscribe({
            next: () => {
              this.notificationService.success('Branch updated successfully.');
              this.loadBranches();
            },
            error: (err) => this.notificationService.error(err.message || 'Failed to update branch'),
          });
        }
      });
  }

  toggleBranchStatus(branch: HospitalBranch): void {
    this.hospitalService.toggleBranchStatus(branch.publicId).subscribe({
      next: (updated) => {
        this.notificationService.info(`Branch "${updated.branchName}" status: ${updated.status}`);
        this.loadBranches();
      },
      error: (err) => this.notificationService.error(err.message || 'Status toggle failed'),
    });
  }

  confirmDeleteBranch(branch: HospitalBranch): void {
    const data: ConfirmDialogData = {
      title: 'Delete Branch',
      message: `Delete branch "${branch.branchName}" (${branch.branchCode})?`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'warn',
    };
    this.dialog
      .open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.hospitalService.deleteMyBranch(branch.publicId).subscribe({
            next: () => {
              this.notificationService.success('Branch deleted successfully.');
              this.loadBranches();
            },
            error: (err) => this.notificationService.error(err.message || 'Delete failed'),
          });
        }
      });
  }

  loadContacts(): void {
    this.hospitalService.getMyContacts().subscribe({
      next: (list) => this.contacts.set(list),
    });
  }

  openAddContactDialog(): void {
    this.dialog
      .open(ContactDialogComponent, {
        data: { title: 'Add Key Contact' },
        width: '450px',
      })
      .afterClosed()
      .subscribe((req) => {
        if (req) {
          this.hospitalService.createMyContact(req).subscribe({
            next: () => {
              this.notificationService.success('Contact added successfully.');
              this.loadContacts();
            },
            error: (err) => this.notificationService.error(err.message || 'Failed to add contact'),
          });
        }
      });
  }

  openEditContactDialog(contact: HospitalContact): void {
    this.dialog
      .open(ContactDialogComponent, {
        data: { contact, title: 'Edit Key Contact' },
        width: '450px',
      })
      .afterClosed()
      .subscribe((req) => {
        if (req) {
          this.hospitalService.updateMyContact(contact.publicId, req).subscribe({
            next: () => {
              this.notificationService.success('Contact updated successfully.');
              this.loadContacts();
            },
            error: (err) => this.notificationService.error(err.message || 'Failed to update contact'),
          });
        }
      });
  }

  confirmDeleteContact(contact: HospitalContact): void {
    const data: ConfirmDialogData = {
      title: 'Delete Contact',
      message: `Delete contact "${contact.name}" (${contact.contactType})?`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'warn',
    };
    this.dialog
      .open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.hospitalService.deleteMyContact(contact.publicId).subscribe({
            next: () => {
              this.notificationService.success('Contact deleted successfully.');
              this.loadContacts();
            },
            error: (err) => this.notificationService.error(err.message || 'Delete failed'),
          });
        }
      });
  }

  loadDocuments(): void {
    this.hospitalService.getMyDocuments().subscribe({
      next: (list) => this.documents.set(list),
    });
  }

  openUploadDocumentDialog(): void {
    this.dialog
      .open(DocumentUploadDialogComponent, { width: '480px' })
      .afterClosed()
      .subscribe((result: DocumentUploadResult | undefined) => {
        if (result) {
          this.hospitalService
            .uploadMyDocument(result.file, result.documentType, result.expiryDate)
            .subscribe({
              next: () => {
                this.notificationService.success('Compliance document uploaded successfully.');
                this.loadDocuments();
              },
              error: (err) => this.notificationService.error(err.message || 'Upload failed'),
            });
        }
      });
  }

  downloadDocument(doc: HospitalDocument): void {
    this.hospitalService.downloadDocument(doc.publicId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.documentName;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => this.notificationService.error(err.message || 'Download failed'),
    });
  }

  confirmDeleteDocument(doc: HospitalDocument): void {
    const data: ConfirmDialogData = {
      title: 'Delete Document',
      message: `Delete compliance document "${doc.documentName}"?`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'warn',
    };
    this.dialog
      .open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.hospitalService.deleteMyDocument(doc.publicId).subscribe({
            next: () => {
              this.notificationService.success('Document deleted successfully.');
              this.loadDocuments();
            },
            error: (err) => this.notificationService.error(err.message || 'Delete failed'),
          });
        }
      });
  }

  loadAudits(): void {
    this.hospitalService.getMyHospitalAudits().subscribe({
      next: (page) => this.auditLogs.set(page.content),
    });
  }
}