import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EmptyStateComponent } from '../empty-state/empty-state';
import { PageHeaderComponent } from '../page-header/page-header';

export interface ModulePlaceholderRouteData {
  moduleName: string;
  moduleIcon?: string;
  description: string;
  targetPhase: string;
}

@Component({
  selector: 'app-module-placeholder',
  standalone: true,
  imports: [PageHeaderComponent, EmptyStateComponent],
  templateUrl: './module-placeholder.html',
  styleUrl: './module-placeholder.scss',
})
export class ModulePlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly data = this.route.snapshot.data as ModulePlaceholderRouteData;

  returnToWorkspace(): void {
    void this.router.navigate(['/dashboard']);
  }
}
