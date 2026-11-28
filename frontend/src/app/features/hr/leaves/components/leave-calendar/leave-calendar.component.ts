import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { NotificationService } from '../../../../../core/services/notification.service';
import { EmptyStateComponent } from '../../../../../shared/components/empty-state/empty-state';
import { LoadingIndicatorComponent } from '../../../../../shared/components/loading-indicator/loading-indicator';
import { PageHeaderComponent } from '../../../../../shared/components/page-header/page-header';
import { Department } from '../../../../hospital/models/hospital.model';
import { HospitalService } from '../../../../hospital/services/hospital.service';
import { LeaveCalendarItem } from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';

@Component({
  selector: 'app-leave-calendar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    PageHeaderComponent,
    LoadingIndicatorComponent,
    EmptyStateComponent,
  ],
  template: `
    <div class="page-container">
      <app-page-header
        title="Clinical Leave Calendar"
        subtitle="Track department leave schedules and employee availability to maintain optimal clinical staffing coverage"
      >
        <button mat-stroked-button (click)="loadCalendar()">
          <mat-icon>refresh</mat-icon> Refresh Calendar
        </button>
      </app-page-header>

      <!-- Date & Dept Filter Bar -->
      <mat-card class="filter-card">
        <mat-card-content class="filter-row">
          <mat-form-field appearance="outline" class="date-field">
            <mat-label>Start Date</mat-label>
            <input matInput type="date" [(ngModel)]="startDate" (change)="loadCalendar()" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="date-field">
            <mat-label>End Date</mat-label>
            <input matInput type="date" [(ngModel)]="endDate" (change)="loadCalendar()" />
          </mat-form-field>

          <mat-form-field appearance="outline" class="dept-field">
            <mat-label>Filter by Department</mat-label>
            <mat-select [ngModel]="selectedDeptName()" (ngModelChange)="selectedDeptName.set($event)">
              <mat-option [value]="null">All Departments</mat-option>
              <mat-option *ngFor="let dept of departments()" [value]="dept.departmentName">
                {{ dept.departmentName }}
              </mat-option>
            </mat-select>
          </mat-form-field>
        </mat-card-content>
      </mat-card>

      <app-loading-indicator *ngIf="isLoading()" message="Loading calendar schedule..."></app-loading-indicator>

      <!-- Calendar Schedule Table -->
      <mat-card *ngIf="!isLoading()" class="table-card">
        <mat-card-content class="p-0">
          <table mat-table [dataSource]="filteredItems()" class="w-full">
            <ng-container matColumnDef="dates">
              <th mat-header-cell *matHeaderCellDef>Dates & Period</th>
              <td mat-cell *matCellDef="let item">
                <div class="font-medium text-dark">{{ item.startDate }} to {{ item.endDate }}</div>
                <div class="text-xs text-muted">
                  {{ item.totalDays }} Day(s)
                  <span *ngIf="item.halfDay" class="text-primary font-semibold">(Half Day)</span>
                </div>
              </td>
            </ng-container>

            <ng-container matColumnDef="employee">
              <th mat-header-cell *matHeaderCellDef>Employee & Department</th>
              <td mat-cell *matCellDef="let item">
                <div class="font-medium text-dark">{{ item.employeeName }}</div>
                <div class="text-xs text-muted">{{ item.employeeNumber }} • {{ item.departmentName || 'General Staff' }}</div>
              </td>
            </ng-container>

            <ng-container matColumnDef="leaveType">
              <th mat-header-cell *matHeaderCellDef>Leave Type</th>
              <td mat-cell *matCellDef="let item">
                <span class="type-badge">{{ item.leaveTypeName }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let item">
                <span [class]="'status-pill ' + (item.status === 'APPROVED' ? 'pill-approved' : 'pill-pending')">
                  {{ item.status }}
                </span>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
          </table>

          <app-empty-state
            *ngIf="filteredItems().length === 0"
            icon="event_available"
            title="Full Attendance Scheduled"
            description="No employees are scheduled on leave during the selected dates and department scope."
          ></app-empty-state>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .filter-card { margin-top: 16px; margin-bottom: 20px; }
    .filter-row { display: flex; gap: 16px; align-items: center; padding: 12px 0; }
    .date-field { flex: 1; }
    .dept-field { flex: 2; }
    .table-card { overflow: hidden; }
    .w-full { width: 100%; }
    .p-0 { padding: 0 !important; }
    .text-dark { color: #1e293b; }
    .text-muted { color: #64748b; }
    .text-primary { color: #1d4ed8; }
    .text-xs { font-size: 0.75rem; }
    .type-badge {
      background: #eff6ff;
      color: #1e40af;
      padding: 3px 8px;
      border-radius: 4px;
      font-weight: 600;
      font-size: 0.8rem;
    }
    .status-pill {
      font-size: 0.75rem;
      padding: 3px 10px;
      border-radius: 12px;
      font-weight: 600;
    }
    .pill-approved { background: #dcfce7; color: #166534; }
    .pill-pending { background: #fef3c7; color: #92400e; }
  `],
})
export class LeaveCalendarComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly hospitalService = inject(HospitalService);
  private readonly notificationService = inject(NotificationService);

  readonly isLoading = signal(true);
  readonly items = signal<LeaveCalendarItem[]>([]);
  readonly departments = signal<Department[]>([]);
  readonly selectedDeptName = signal<string | null>(null);

  readonly filteredItems = computed(() => {
    const dept = this.selectedDeptName();
    const all = this.items();
    if (!dept) return all;
    return all.filter((item) => item.departmentName === dept);
  });

  startDate = '';
  endDate = '';

  readonly displayedColumns: string[] = ['dates', 'employee', 'leaveType', 'status'];

  ngOnInit(): void {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);

    this.startDate = firstDay.toISOString().split('T')[0];
    this.endDate = lastDay.toISOString().split('T')[0];

    this.loadDepartments();
    this.loadCalendar();
  }

  loadDepartments(): void {
    this.hospitalService.getMyDepartments().subscribe({
      next: (depts: Department[]) => this.departments.set(depts),
    });
  }

  loadCalendar(): void {
    this.isLoading.set(true);
    this.leaveService.getLeaveCalendar(this.startDate, this.endDate).subscribe({
      next: (items) => {
        this.items.set(items);
        this.isLoading.set(false);
      },
      error: () => {
        this.notificationService.error('Failed to load leave calendar');
        this.isLoading.set(false);
      },
    });
  }
}
