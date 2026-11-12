import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { ChangePasswordDialogComponent } from '../../shared/components/change-password-dialog/change-password-dialog';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator';
import { ConfirmDialogData } from '../../shared/models/confirm-dialog.model';
import { APP_CONSTANTS } from '../constants/app.constants';
import { NAVIGATION_ITEMS } from '../constants/navigation.constants';
import { NavItem } from '../models/navigation.model';
import { AuthService } from '../services/auth.service';
import { AuthStateService } from '../services/auth-state.service';
import { BreadcrumbService } from '../services/breadcrumb.service';
import { LoadingService } from '../services/loading.service';

interface NavGroup {
  label: string;
  items: NavItem[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatMenuModule,
    LoadingIndicatorComponent,
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly loadingService = inject(LoadingService);
  private readonly breadcrumbService = inject(BreadcrumbService);
  private readonly authState = inject(AuthStateService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  readonly appName = APP_CONSTANTS.applicationName;
  readonly appSubtitle = APP_CONSTANTS.applicationSubtitle;
  readonly isLoading = this.loadingService.isLoading;
  readonly breadcrumbs = this.breadcrumbService.breadcrumbs;
  readonly currentUser = this.authState.currentUser;

  readonly tenantLabel = computed(() => {
    const user = this.currentUser();
    if (!user) {
      return '';
    }
    return user.userScope === 'PLATFORM'
      ? 'Platform Scope'
      : user.hospitalName || user.hospitalCode || `Hospital #${user.hospitalId}`;
  });

  readonly primaryRoleLabel = computed(() => {
    const roles = this.currentUser()?.roles ?? [];
    return roles.length > 0 ? roles[0].replaceAll('_', ' ') : 'Authenticated';
  });

  readonly isCompactViewport = toSignal(
    this.breakpointObserver
      .observe('(max-width: 959.98px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false }
  );

  readonly sidebarExpanded = signal(true);
  readonly sidenavMode = computed(() => (this.isCompactViewport() ? 'over' : 'side'));
  readonly sidenavOpened = computed(() =>
    this.isCompactViewport() ? this.mobileDrawerOpen() : this.sidebarExpanded()
  );

  private readonly mobileDrawerOpen = signal(false);

  private readonly groupDefinitions: readonly { label: string; section: NavItem['section'] }[] = [
    { label: 'Platform', section: 'core' },
    { label: 'Clinical Care', section: 'clinical' },
    { label: 'Operations', section: 'operations' },
    { label: 'Administration', section: 'administration' },
  ];

  readonly navGroups = computed<readonly NavGroup[]>(() => {
    this.currentUser(); // track reactive dependency
    return this.groupDefinitions
      .map((group) => ({
        label: group.label,
        items: NAVIGATION_ITEMS.filter(
          (item) =>
            item.section === group.section &&
            this.authState.canAccess(item.requiredPermissions, item.requiredScope)
        ),
      }))
      .filter((group) => group.items.length > 0);
  });

  toggleSidebar(): void {
    if (this.isCompactViewport()) {
      this.mobileDrawerOpen.update((open) => !open);
    } else {
      this.sidebarExpanded.update((open) => !open);
    }
  }

  closeMobileDrawer(): void {
    if (this.isCompactViewport()) {
      this.mobileDrawerOpen.set(false);
    }
  }

  openChangePasswordDialog(): void {
    this.dialog.open(ChangePasswordDialogComponent, { width: '440px' });
  }

  confirmLogout(): void {
    const data: ConfirmDialogData = {
      title: 'Sign Out',
      message: 'End your current CareOS-HMS session on this browser?',
      confirmLabel: 'Sign Out',
      cancelLabel: 'Cancel',
      severity: 'primary',
    };

    this.dialog
      .open(ConfirmDialogComponent, { data, width: '400px' })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (confirmed) {
          this.authService.logout().subscribe();
        }
      });
  }

  confirmLogoutAll(): void {
    const data: ConfirmDialogData = {
      title: 'Sign Out All Sessions',
      message: 'Revoke all active refresh sessions across all browsers and devices?',
      confirmLabel: 'Sign Out All',
      cancelLabel: 'Cancel',
      severity: 'warn',
    };

    this.dialog
      .open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (confirmed) {
          this.authService.logoutAll().subscribe();
        }
      });
  }
}
