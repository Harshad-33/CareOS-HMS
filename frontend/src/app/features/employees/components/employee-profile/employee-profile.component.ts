import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header';
import {
  Employee,
  EmployeeLifecycleEvent,
  EmployeeSummary,
  UpdateEmployeeRequest,
  UpdateEmployeeStatusRequest,
} from '../../models/employee.model';
import { EmployeeService } from '../../services/employee.service';
import {
  EmployeeAccountDialogComponent,
  EmployeeAccountDialogResult,
} from '../employee-account-dialog/employee-account-dialog.component';
import {
  EmployeeEditDialogComponent,
} from '../employee-edit-dialog/employee-edit-dialog.component';
import { EmployeeStatusDialogComponent } from '../employee-status-dialog/employee-status-dialog.component';

@Component({
  selector: 'app-employee-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    PageHeaderComponent,
    EmptyStateComponent,
  ],
  templateUrl: './employee-profile.component.html',
  styleUrls: ['./employee-profile.component.scss'],
})
export class EmployeeProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);
  private readonly authStateService = inject(AuthStateService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly employee = signal<Employee | null>(null);
  readonly lifecycleEvents = signal<EmployeeLifecycleEvent[]>([]);
  readonly loading = signal<boolean>(true);
  readonly profilePhotoUrl = signal<string | null>(null);
  readonly activeTabIndex = signal<number>(0);

  readonly canManage = computed(() =>
    this.authStateService.hasPermission('HOSPITAL_EMPLOYEES_MANAGE')
  );
  readonly canChangeStatus = computed(() =>
    this.authStateService.hasPermission('HOSPITAL_EMPLOYEES_STATUS')
  );
  readonly canLinkAccount = computed(() =>
    this.authStateService.hasPermission('HOSPITAL_EMPLOYEES_ACCOUNT_LINK')
  );

  ngOnInit(): void {
    const publicId = this.route.snapshot.paramMap.get('publicId');
    if (!publicId) {
      this.notificationService.error('Invalid employee identifier');
      this.router.navigate(['/employees']);
      return;
    }
    this.loadEmployee(publicId);
  }

  loadEmployee(publicId: string): void {
    this.loading.set(true);
    this.employeeService.getEmployeeByPublicId(publicId).subscribe({
      next: (emp) => {
        this.employee.set(emp);
        this.loading.set(false);

        if (emp.profilePhotoKey) {
          this.loadPhoto(publicId);
        } else {
          this.profilePhotoUrl.set(null);
        }

        this.loadLifecycleHistory(publicId);
      },
      error: () => {
        this.loading.set(false);
        this.notificationService.error('Failed to load employee profile');
      },
    });
  }

  loadPhoto(publicId: string): void {
    this.employeeService.getProfilePhotoBlob(publicId).subscribe({
      next: (blob) => {
        if (blob && blob.size > 0) {
          this.profilePhotoUrl.set(URL.createObjectURL(blob));
        }
      },
      error: () => {
        // Silently keep placeholder if photo fetch fails
      },
    });
  }

  loadLifecycleHistory(publicId: string): void {
    this.employeeService.getLifecycleHistory(publicId).subscribe({
      next: (events) => {
        this.lifecycleEvents.set(events);
      },
      error: () => {
        // Silently retain empty events
      },
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const emp = this.employee();
    if (!input.files || input.files.length === 0 || !emp) {
      return;
    }

    const file = input.files[0];
    if (file.size > 10 * 1024 * 1024) {
      this.notificationService.error('Profile photo must be less than 10MB');
      return;
    }

    this.employeeService.uploadProfilePhoto(emp.publicId, file).subscribe({
      next: (updated) => {
        this.employee.set(updated);
        this.loadPhoto(updated.publicId);
        this.notificationService.success('Profile photo updated successfully.');
      },
      error: () => {
        this.notificationService.error('Failed to upload profile photo');
      },
    });
  }

  openEditDialog(focusSection: 'personal' | 'professional' = 'personal'): void {
    const emp = this.employee();
    if (!emp) return;

    const dialogRef = this.dialog.open(EmployeeEditDialogComponent, {
      width: '760px',
      data: {
        employee: emp,
        focusSection,
      },
    });

    dialogRef.afterClosed().subscribe((result: UpdateEmployeeRequest | undefined) => {
      if (result) {
        this.employeeService.updateEmployee(emp.publicId, result).subscribe({
          next: (updated) => {
            this.employee.set(updated);
            this.notificationService.success('Employee profile updated successfully.');
          },
          error: (err) => {
            const msg = err?.error?.message || 'Failed to update employee profile';
            this.notificationService.error(msg);
          },
        });
      }
    });
  }

  openStatusDialog(): void {
    const emp = this.employee();
    if (!emp) return;

    const summary: EmployeeSummary = {
      publicId: emp.publicId,
      employeeNumber: emp.employeeNumber,
      firstName: emp.firstName,
      lastName: emp.lastName,
      fullName: emp.fullName,
      gender: emp.gender,
      designation: emp.designation,
      employmentType: emp.employmentType,
      status: emp.status,
      email: emp.email,
      phoneNumber: emp.phoneNumber,
      joiningDate: emp.joiningDate,
      hasUserAccount: emp.hasUserAccount,
      departmentName: emp.departmentName,
      branchName: emp.branchName,
    };

    const dialogRef = this.dialog.open(EmployeeStatusDialogComponent, {
      width: '480px',
      data: { employee: summary },
    });

    dialogRef.afterClosed().subscribe((req: UpdateEmployeeStatusRequest | undefined) => {
      if (req) {
        this.employeeService.updateEmployeeStatus(emp.publicId, req).subscribe({
          next: (updated) => {
            this.employee.set(updated);
            this.loadLifecycleHistory(emp.publicId);
            this.notificationService.success(`Status updated to ${req.status}`);
          },
          error: (err) => {
            const msg = err?.error?.message || 'Failed to update employee status';
            this.notificationService.error(msg);
          },
        });
      }
    });
  }

  openAccountDialog(): void {
    const emp = this.employee();
    if (!emp) return;

    const summary: EmployeeSummary = {
      publicId: emp.publicId,
      employeeNumber: emp.employeeNumber,
      firstName: emp.firstName,
      lastName: emp.lastName,
      fullName: emp.fullName,
      gender: emp.gender,
      designation: emp.designation,
      employmentType: emp.employmentType,
      status: emp.status,
      email: emp.email,
      phoneNumber: emp.phoneNumber,
      joiningDate: emp.joiningDate,
      hasUserAccount: emp.hasUserAccount,
      username: emp.userAccount?.username,
      userPublicId: emp.userAccount?.publicId,
    };

    const dialogRef = this.dialog.open(EmployeeAccountDialogComponent, {
      width: '520px',
      data: { employee: summary },
    });

    dialogRef.afterClosed().subscribe((res: EmployeeAccountDialogResult | undefined) => {
      if (!res) return;

      if (res.action === 'link') {
        this.employeeService.linkUserAccount(emp.publicId, res.request).subscribe({
          next: (updated) => {
            this.employee.set(updated);
            this.notificationService.success('User account provisioned and linked successfully.');
          },
          error: (err) => {
            const msg = err?.error?.message || 'Failed to provision user account';
            this.notificationService.error(msg);
          },
        });
      } else if (res.action === 'unlink') {
        this.employeeService.unlinkUserAccount(emp.publicId).subscribe({
          next: (updated) => {
            this.employee.set(updated);
            this.notificationService.success('User account unlinked and sessions revoked.');
          },
          error: (err) => {
            const msg = err?.error?.message || 'Failed to unlink user account';
            this.notificationService.error(msg);
          },
        });
      }
    });
  }

  confirmDelete(): void {
    const emp = this.employee();
    if (!emp) return;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '440px',
      data: {
        title: 'Delete Employee',
        message: `Are you sure you want to delete employee "${emp.fullName}" (${emp.employeeNumber})? This will soft-delete their profile and revoke system credentials.`,
        confirmLabel: 'Delete Employee',
        cancelLabel: 'Cancel',
        severity: 'warn',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.employeeService.deleteEmployee(emp.publicId).subscribe({
          next: () => {
            this.notificationService.success('Employee record deleted.');
            this.router.navigate(['/employees']);
          },
          error: (err) => {
            const msg = err?.error?.message || 'Failed to delete employee';
            this.notificationService.error(msg);
          },
        });
      }
    });
  }

  backToDirectory(): void {
    this.router.navigate(['/employees']);
  }

  calculateAge(dobString?: string): number | string {
    if (!dobString) return '—';
    const dob = new Date(dobString);
    if (isNaN(dob.getTime())) return '—';
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    return age >= 0 ? age : '—';
  }
}
