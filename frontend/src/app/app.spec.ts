import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { AppShell } from './core/layout/app-shell';
import { BreadcrumbService } from './core/services/breadcrumb.service';

describe('CareOS-HMS Shell & Routing', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('should create the root app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render header, brand title, and sidebar navigation items', async () => {
    const fixture = TestBed.createComponent(AppShell);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.shell-brand__name')?.textContent).toContain('CareOS-HMS');
    expect(compiled.querySelectorAll('.sidebar-item').length).toBe(14);
  });

  it('should redirect root path to /dashboard and update breadcrumbs for lazy-loaded routes', async () => {
    const router = TestBed.inject(Router);
    const location = TestBed.inject(Location);
    const breadcrumbService = TestBed.inject(BreadcrumbService);

    await router.navigateByUrl('/');
    expect(location.path()).toBe('/dashboard');
    expect(breadcrumbService.breadcrumbs()).toEqual([
      { label: 'Workspace Overview', route: '/dashboard' },
    ]);

    await router.navigateByUrl('/patients');
    expect(location.path()).toBe('/patients');
    expect(breadcrumbService.breadcrumbs()).toEqual([
      { label: 'Patients', route: '/patients' },
    ]);
  });
});
