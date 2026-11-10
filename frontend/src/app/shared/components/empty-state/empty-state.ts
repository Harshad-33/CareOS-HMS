import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.scss',
})
export class EmptyStateComponent {
  readonly icon = input<string>('inbox');
  readonly title = input<string>('No records available');
  readonly description = input<string>('');
  readonly actionLabel = input<string>('');
  readonly action = output<void>();
}
