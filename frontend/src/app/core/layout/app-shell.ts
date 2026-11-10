import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { LoadingIndicatorComponent } from '../../shared/components/loading-indicator/loading-indicator';
import { APP_CONSTANTS } from '../constants/app.constants';
import { NAVIGATION_ITEMS } from '../constants/navigation.constants';
import { NavItem } from '../models/navigation.model';
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
    LoadingIndicatorComponent,
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly loadingService = inject(LoadingService);
  private readonly breadcrumbService = inject(BreadcrumbService);

  readonly appName = APP_CONSTANTS.applicationName;
  readonly appSubtitle = APP_CONSTANTS.applicationSubtitle;
  readonly isLoading = this.loadingService.isLoading;
  readonly breadcrumbs = this.breadcrumbService.breadcrumbs;

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

  readonly navGroups: readonly NavGroup[] = [
    {
      label: 'Platform',
      items: NAVIGATION_ITEMS.filter((item) => item.section === 'core'),
    },
    {
      label: 'Clinical Care',
      items: NAVIGATION_ITEMS.filter((item) => item.section === 'clinical'),
    },
    {
      label: 'Operations',
      items: NAVIGATION_ITEMS.filter((item) => item.section === 'operations'),
    },
    {
      label: 'Administration',
      items: NAVIGATION_ITEMS.filter((item) => item.section === 'administration'),
    },
  ];

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
}
