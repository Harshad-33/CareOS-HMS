import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [PageHeaderComponent, EmptyStateComponent],
  templateUrl: './unauthorized.html',
  styleUrl: './unauthorized.scss',
})
export class UnauthorizedComponent {
  private readonly router = inject(Router);

  returnToWorkspace(): void {
    void this.router.navigate(['/dashboard']);
  }
}
