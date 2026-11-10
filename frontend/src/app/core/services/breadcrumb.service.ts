import { Injectable, inject, signal } from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { BreadcrumbItem } from '../models/navigation.model';

@Injectable({
  providedIn: 'root',
})
export class BreadcrumbService {
  private readonly router = inject(Router);
  private readonly itemsSignal = signal<BreadcrumbItem[]>([]);

  readonly breadcrumbs = this.itemsSignal.asReadonly();

  constructor() {
    this.updateBreadcrumbs();
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.updateBreadcrumbs());
  }

  private updateBreadcrumbs(): void {
    const items: BreadcrumbItem[] = [];
    this.collectBreadcrumbs(this.router.routerState.snapshot.root, '', items);
    this.itemsSignal.set(items);
  }

  private collectBreadcrumbs(
    route: ActivatedRouteSnapshot,
    accumulatedUrl: string,
    items: BreadcrumbItem[]
  ): void {
    const routeUrl = route.url.map((segment) => segment.path).join('/');
    const nextUrl = routeUrl ? `${accumulatedUrl}/${routeUrl}` : accumulatedUrl;

    const label = route.data['breadcrumb'] as string | undefined;
    if (label) {
      const resolvedUrl = nextUrl || '/';
      const alreadyAdded = items.some(
        (existing) => existing.label === label && existing.route === resolvedUrl
      );
      if (!alreadyAdded) {
        items.push({ label, route: resolvedUrl });
      }
    }

    if (route.firstChild) {
      this.collectBreadcrumbs(route.firstChild, nextUrl, items);
    }
  }
}
