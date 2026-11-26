import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import {
  AdjustLeaveBalanceRequest,
  AllocateLeaveBalanceRequest,
  LeaveBalance,
  LeaveBalanceTransaction,
  LeaveType,
} from '../../models/leave.models';
import { LeaveService } from '../../services/leave.service';
import { EmployeeService } from '../../../../employees/services/employee.service';
import { EmployeeSummary } from '../../../../employees/models/employee.model';
import { NotificationService } from '../../../../../core/services/notification.service';
import { PageHeaderComponent } from '../../../../../shared/components/page-header/page-header';
import { LoadingIndicatorComponent } from '../../../../../shared/components/loading-indicator/loading-indicator';
import { EmptyStateComponent } from '../../../../../shared/components/empty-state/empty-state';
import { LeaveBalanceDialogComponent } from '../leave-balance-dialog/leave-balance-dialog.component';

@Component({
  selector: 'app-leave-balance-list',
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
    MatTooltipModule,
    MatTabsModule,
    PageHeaderComponent,
    LoadingIndicatorComponent,
    EmptyStateComponent,
  ],
  template: `
    <div class="page-container">
      <app-page-header
        title="Leave Balance & Ledger Management"
        subtitle="Allocate annual leave balances, make manual adjustments, and inspect immutable transaction audit ledgers"
      >
        <button mat-flat-button color="primary" (click)="openAllocateDialog()">
          <mat-icon>add_card</mat-icon>
          Allocate Balance
        </button>
      </app-page-header>

      <!-- Employee Selector Card -->
      <mat-card class="filter-card">
        <mat-card-content class="filter-row">
          <mat-form-field appearance="outline" class="emp-select">
            <mat-label>Select Employee to View Balances & Ledger</mat-label>
            <mat-select [(ngModel)]="selectedEmpPubId" (selectionChange)="onEmployeeSelected($event.value)">
              <mat-option *ngFor="let emp of employees()" [value]="emp.publicId">
                {{ emp.fullName }} ({{ emp.employeeNumber }} • {{ emp.designation }})
              </mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="year-select">
            <mat-label>Year</mat-label>
            <input matInput type="number" [(ngModel)]="selectedYear" (change)="reloadBalances()" />
          </mat-form-field>
        </mat-card-content>
      </mat-card>

      <app-loading-indicator *ngIf="isLoading()" message="Loading balance records..."></app-loading-indicator>

      <!-- Selected Employee Details -->
      <ng-container *ngIf="!isLoading() && selectedEmployee()">
        <!-- Balances Grid -->
        <div class="balances-grid">
          <mat-card *ngFor="let bal of balances()" class="balance-card">
            <div class="card-header-bar">
              <span class="type-name">{{ bal.leaveTypeName }}</span>
              <span class="type-code">{{ bal.leaveTypeCode }}</span>
            </div>
            <mat-card-content class="card-body">
              <div class="avail-num">{{ bal.availableBalance }}</div>
              <div class="avail-label">Available Days</div>

              <div class="metrics-row">
                <div class="metric-col">
                  <span class="val">{{ bal.totalAllocated }}</span>
                  <span class="lbl">Allocated</span>
                </div>
                <div class="metric-col">
                  <span class="val text-danger">{{ bal.usedDays }}</span>
                  <span class="lbl">Used</span>
                </div>
                <div class="metric-col">
                  <span class="val text-warn">{{ bal.pendingApprovalDays }}</span>
                  <span class="lbl">Pending</span>
                </div>
                <div class="metric-col">
                  <span class="val text-muted">{{ bal.carriedForward }}</span>
                  <span class="lbl">Carried Fwd</span>
                </div>
              </div>

              <button mat-stroked-button color="primary" class="adjust-btn" (click)="openAdjustDialog(bal)">
                <mat-icon>tune</mat-icon> Adjust Balance
              </button>
            </mat-card-content>
          </mat-card>
        </div>

        <app-empty-state
          *ngIf="balances().length === 0"
          icon="account_balance_wallet"
          title="No Balances Allocated"
          [description]="'No leave balances allocated for this employee in ' + selectedYear + '. Click Allocate Balance above to allocate.'"
        ></app-empty-state>

        <!-- Transaction Ledger Table -->
        <mat-card class="ledger-card" *ngIf="transactions().length > 0">
          <mat-card-header class="ledger-header">
            <mat-card-title class="ledger-title">
              <mat-icon>history</mat-icon>
              Transaction Audit Ledger
            </mat-card-title>
          </mat-card-header>
          <mat-card-content class="p-0">
            <table mat-table [dataSource]="transactions()" class="w-full">
              <ng-container matColumnDef="timestamp">
                <th mat-header-cell *matHeaderCellDef>Date & Time</th>
                <td mat-cell *matCellDef="let tx">{{ tx.transactionTimestamp | date:'medium' }}</td>
              </ng-container>

              <ng-container matColumnDef="leaveType">
                <th mat-header-cell *matHeaderCellDef>Leave Type</th>
                <td mat-cell *matCellDef="let tx">{{ tx.leaveTypeName }}</td>
              </ng-container>

              <ng-container matColumnDef="type">
                <th mat-header-cell *matHeaderCellDef>Transaction Type</th>
                <td mat-cell *matCellDef="let tx">
                  <span class="tx-badge">{{ tx.transactionType }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="days">
                <th mat-header-cell *matHeaderCellDef>Days</th>
                <td mat-cell *matCellDef="let tx">
                  <strong [class.text-success]="tx.days > 0" [class.text-danger]="tx.days < 0">
                    {{ tx.days > 0 ? '+' : '' }}{{ tx.days }}
                  </strong>
                </td>
              </ng-container>

              <ng-container matColumnDef="balanceAfter">
                <th mat-header-cell *matHeaderCellDef>Balance After</th>
                <td mat-cell *matCellDef="let tx"><strong>{{ tx.balanceAfter }}</strong></td>
              </ng-container>

              <ng-container matColumnDef="reason">
                <th mat-header-cell *matHeaderCellDef>Reason & Performed By</th>
                <td mat-cell *matCellDef="let tx">
                  <div class="text-dark">{{ tx.reason }}</div>
                  <div class="text-muted text-xs">by {{ tx.performedBy }}</div>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="ledgerColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: ledgerColumns"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      </ng-container>

      <app-empty-state
        *ngIf="!isLoading() && !selectedEmployee()"
        icon="people"
        title="Select an Employee"
        description="Select an employee from the dropdown above to view their leave balances and transaction history."
      ></app-empty-state>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .filter-card { margin-top: 16px; margin-bottom: 20px; }
    .filter-row { display: flex; gap: 16px; align-items: center; padding: 16px 0; }
    .emp-select { flex: 3; }
    .year-select { flex: 1; }
    .balances-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .balance-card {
      border: 1px solid #e0e0e0;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .card-header-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #f8fafc;
      padding: 10px 16px;
      border-bottom: 1px solid #e2e8f0;
    }
    .type-name { font-weight: 600; color: #1e293b; }
    .type-code { font-size: 0.75rem; background: #e2e8f0; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
    .card-body {
      padding: 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .avail-num { font-size: 2.2rem; font-weight: 700; color: #1e40af; line-height: 1; }
    .avail-label { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.5px; color: #64748b; margin-top: 4px; margin-bottom: 16px; }
    .metrics-row {
      display: flex;
      justify-content: space-between;
      width: 100%;
      background: #f8fafc;
      padding: 8px 12px;
      border-radius: 6px;
      margin-bottom: 14px;
    }
    .metric-col { display: flex; flex-direction: column; align-items: center; }
    .metric-col .val { font-size: 0.95rem; font-weight: 600; }
    .metric-col .lbl { font-size: 0.7rem; color: #64748b; text-transform: uppercase; }
    .text-danger { color: #dc2626; }
    .text-warn { color: #d97706; }
    .text-success { color: #16a34a; }
    .text-muted { color: #64748b; }
    .text-xs { font-size: 0.75rem; }
    .text-dark { color: #1e293b; }
    .adjust-btn { width: 100%; }
    .ledger-card { margin-top: 24px; overflow: hidden; }
    .ledger-header { padding: 16px 20px 8px 20px; }
    .ledger-title { display: flex; align-items: center; gap: 8px; font-size: 1.1rem; }
    .w-full { width: 100%; }
    .p-0 { padding: 0 !important; }
    .tx-badge {
      font-size: 0.75rem;
      background: #f1f5f9;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 600;
      color: #334155;
    }
  `],
})
export class LeaveBalanceListComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);
  private readonly employeeService = inject(EmployeeService);
  private readonly notificationService = inject(NotificationService);
  private readonly dialog = inject(MatDialog);

  readonly isLoading = signal(false);
  readonly employees = signal<EmployeeSummary[]>([]);
  readonly leaveTypes = signal<LeaveType[]>([]);
  readonly balances = signal<LeaveBalance[]>([]);
  readonly transactions = signal<LeaveBalanceTransaction[]>([]);
  readonly selectedEmployee = signal<EmployeeSummary | null>(null);

  selectedEmpPubId: string | null = null;
  selectedYear: number = new Date().getFullYear();

  readonly ledgerColumns: string[] = ['timestamp', 'leaveType', 'type', 'days', 'balanceAfter', 'reason'];

  ngOnInit(): void {
    this.loadInitialData();
  }

  loadInitialData(): void {
    this.isLoading.set(true);
    this.employeeService.getEmployees({ size: 100 }).subscribe({
      next: (res) => {
        this.employees.set(res.content);
        if (res.content.length > 0) {
          this.selectedEmpPubId = res.content[0].publicId;
          this.onEmployeeSelected(res.content[0].publicId);
        } else {
          this.isLoading.set(false);
        }
      },
      error: () => {
        this.notificationService.error('Failed to load employees');
        this.isLoading.set(false);
      },
    });

    this.leaveService.getActiveLeaveTypes().subscribe({
      next: (types) => this.leaveTypes.set(types),
    });
  }

  onEmployeeSelected(pubId: string): void {
    const emp = this.employees().find((e) => e.publicId === pubId) || null;
    this.selectedEmployee.set(emp);
    this.reloadBalances();
  }

  reloadBalances(): void {
    if (!this.selectedEmpPubId) return;

    this.isLoading.set(true);
    this.leaveService.getEmployeeBalances(this.selectedEmpPubId, this.selectedYear).subscribe({
      next: (bals) => {
        this.balances.set(bals);
        this.loadTransactions();
      },
      error: () => {
        this.notificationService.error('Failed to load employee balances');
        this.isLoading.set(false);
      },
    });
  }

  loadTransactions(): void {
    if (!this.selectedEmpPubId) {
      this.isLoading.set(false);
      return;
    }

    this.leaveService.getEmployeeTransactions(this.selectedEmpPubId).subscribe({
      next: (page) => {
        this.transactions.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  openAllocateDialog(): void {
    const ref = this.dialog.open(LeaveBalanceDialogComponent, {
      width: '520px',
      data: {
        mode: 'ALLOCATE',
        leaveTypes: this.leaveTypes(),
        employees: this.employees(),
      },
    });

    ref.afterClosed().subscribe((res: AllocateLeaveBalanceRequest | undefined) => {
      if (!res) return;
      this.leaveService.allocateBalance(res).subscribe({
        next: () => {
          this.notificationService.success('Leave balance allocated successfully');
          if (res.employeePublicId === this.selectedEmpPubId) {
            this.reloadBalances();
          }
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to allocate balance');
        },
      });
    });
  }

  openAdjustDialog(balance: LeaveBalance): void {
    const ref = this.dialog.open(LeaveBalanceDialogComponent, {
      width: '520px',
      data: {
        mode: 'ADJUST',
        balance,
      },
    });

    ref.afterClosed().subscribe((res: AdjustLeaveBalanceRequest | undefined) => {
      if (!res) return;
      this.leaveService.adjustBalance(balance.publicId, res).subscribe({
        next: () => {
          this.notificationService.success('Balance adjusted successfully');
          this.reloadBalances();
        },
        error: (err) => {
          this.notificationService.error(err.error?.message || 'Failed to adjust balance');
        },
      });
    });
  }
}
