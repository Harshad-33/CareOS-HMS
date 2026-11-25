import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PERMISSIONS } from '../../../../core/constants/permissions.constants';
import { AuthStateService } from '../../../../core/services/auth-state.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state';
import { LoadingIndicatorComponent } from '../../../../shared/components/loading-indicator/loading-indicator';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header';
import { ConfirmDialogData } from '../../../../shared/models/confirm-dialog.model';
import { Department } from '../../../hospital/models/hospital.model';
import { HospitalService } from '../../../hospital/services/hospital.service';
import {
  EmployeeStatus,
  EmployeeSummary,
  EmploymentType,
  UpdateEmployeeStatusRequest,
} from '../../models/employee.model';
import { EmployeeService } from '../../services/employee.service';
import {
  EmployeeAccountDialogComponent,
  EmployeeAccountDialogResult,
} from '../employee-account-dialog/employee-account-dialog.component';
import { EmployeeStatusDialogComponent } from '../employee-status-dialog/employee-status-dialog.component';

@Component({
  selector: 'app-employee-directory',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatTooltipModule,
    MatDividerModule,
    PageHeaderComponent,
    EmptyStateComponent,
    LoadingIndicatorComponent,
  ],
  templateUrl: './employee-directory.component.html',
  styleUrl: './employee-directory.component.scss',
})
export class EmployeeDirectoryComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly hospitalService = inject(HospitalService);
  private readonly authState = inject(AuthStateService);
  private readonly notification = inject(NotificationService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);

  readonly displayedColumns: string[] = [
    'avatar',
    'employeeInfo',
    'department',
    'employmentType',
    'contact',
    'account',
    'status',
    'actions',
  ];

  // State Signals
  readonly employees = signal<EmployeeSummary[]>([]);
  readonly totalEmployees = signal(0);
  readonly loading = signal(false);
  readonly departments = signal<Department[]>([]);

  // Filter Signals
  readonly searchQuery = signal('');
  readonly filterDepartment = signal<string | undefined>(undefined);
  readonly filterEmploymentType = signal<EmploymentType | undefined>(undefined);
  readonly filterStatus = signal<EmployeeStatus | undefined>(undefined);

  // Pagination Signals
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);

  // Computed Permissions
  readonly canManage = computed(() =>
    this.authState.hasPermission(PERMISSIONS.HOSPITAL_EMPLOYEES_MANAGE)
  );
  readonly canChangeStatus = computed(() =>
    this.authState.hasPermission(PERMISSIONS.HOSPITAL_EMPLOYEES_STATUS)
  );
  readonly canLinkAccount = computed(() =>
    this.authState.hasPermission(PERMISSIONS.HOSPITAL_EMPLOYEES_ACCOUNT_LINK)
  );
  readonly hasActiveFilters = computed(
    () =>
      !!this.searchQuery() ||
      !!this.filterDepartment() ||
      !!this.filterEmploymentType() ||
      !!this.filterStatus()
  );

  ngOnInit(): void {
    this.loadDepartments();
    this.loadEmployees();
  }

  loadDepartments(): void {
    this.hospitalService.getMyDepartments().subscribe({
      next: (depts) => this.departments.set(depts),
      error: () => {}, // Department filter is optional
    });
  }

  loadEmployees(): void {
    this.loading.set(true);
    this.employeeService
      .getEmployees({
        search: this.searchQuery() || undefined,
        departmentPublicId: this.filterDepartment(),
        employmentType: this.filterEmploymentType(),
        status: this.filterStatus(),
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .subscribe({
        next: (page) => {
          this.employees.set(page.content);
          this.totalEmployees.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.notification.error(err?.error?.message || 'Failed to load employees');
        },
      });
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.pageIndex.set(0);
    this.loadEmployees();
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.pageIndex.set(0);
    this.loadEmployees();
  }

  onDepartmentChange(value: string | undefined): void {
    this.filterDepartment.set(value);
    this.pageIndex.set(0);
    this.loadEmployees();
  }

  onEmploymentTypeChange(value: EmploymentType | undefined): void {
    this.filterEmploymentType.set(value);
    this.pageIndex.set(0);
    this.loadEmployees();
  }

  onStatusChange(value: EmployeeStatus | undefined): void {
    this.filterStatus.set(value);
    this.pageIndex.set(0);
    this.loadEmployees();
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.filterDepartment.set(undefined);
    this.filterEmploymentType.set(undefined);
    this.filterStatus.set(undefined);
    this.pageIndex.set(0);
    this.loadEmployees();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadEmployees();
  }

  registerEmployee(): void {
    this.router.navigate(['/employees/register']);
  }

  viewProfile(emp: EmployeeSummary): void {
    this.router.navigate(['/employees', emp.publicId]);
  }

  openStatusDialog(emp: EmployeeSummary): void {
    const dialogRef = this.dialog.open(EmployeeStatusDialogComponent, {
      width: '480px',
      data: { employee: emp },
    });

    dialogRef.afterClosed().subscribe((result: UpdateEmployeeStatusRequest | undefined) => {
      if (result) {
        this.employeeService.updateEmployeeStatus(emp.publicId, result).subscribe({
          next: () => {
            this.notification.success(`Status updated for ${emp.fullName}`);
            this.loadEmployees();
          },
          error: (err) => {
            this.notification.error(err?.error?.message || 'Failed to update employee status');
          },
        });
      }
    });
  }

  openAccountDialog(emp: EmployeeSummary): void {
    const dialogRef = this.dialog.open(EmployeeAccountDialogComponent, {
      width: '500px',
      data: { employee: emp },
    });

    dialogRef.afterClosed().subscribe((result: EmployeeAccountDialogResult | undefined) => {
      if (!result) return;

      if (result.action === 'link') {
        this.employeeService.linkUserAccount(emp.publicId, result.request).subscribe({
          next: () => {
            this.notification.success(`User account provisioned and linked for ${emp.fullName}`);
            this.loadEmployees();
          },
          error: (err) => {
            this.notification.error(err?.error?.message || 'Failed to link user account');
          },
        });
      } else if (result.action === 'unlink') {
        this.employeeService.unlinkUserAccount(emp.publicId).subscribe({
          next: () => {
            this.notification.success(`User account unlinked from ${emp.fullName}`);
            this.loadEmployees();
          },
          error: (err) => {
            this.notification.error(err?.error?.message || 'Failed to unlink user account');
          },
        });
      }
    });
  }

  confirmDelete(emp: EmployeeSummary): void {
    const dialogData: ConfirmDialogData = {
      title: 'Delete Employee Record',
      message: `Are you sure you want to delete employee ${emp.fullName} (${emp.employeeNumber})? This will soft-delete the employee profile and revoke any linked user account access.`,
      confirmLabel: 'Delete Employee',
      cancelLabel: 'Cancel',
      severity: 'warn',
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '450px',
      data: dialogData,
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.employeeService.deleteEmployee(emp.publicId).subscribe({
          next: () => {
            this.notification.success(`Employee ${emp.fullName} has been deleted`);
            this.loadEmployees();
          },
          error: (err) => {
            this.notification.error(err?.error?.message || 'Failed to delete employee');
          },
        });
      }
    });
  }

  getInitials(name: string): string {
    if (!name) return 'EMP';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  formatEmploymentType(type: EmploymentType): string {
    switch (type) {
      case 'FULL_TIME':
        return 'Full Time';
      case 'PART_TIME':
        return 'Part Time';
      case 'CONTRACT':
        return 'Contract';
      case 'PROBATION':
        return 'Probation';
      case 'INTERN':
        return 'Intern';
      case 'VISITING_CONSULTANT':
        return 'Visiting Consultant';
      default:
        return type;
    }
  }
}
