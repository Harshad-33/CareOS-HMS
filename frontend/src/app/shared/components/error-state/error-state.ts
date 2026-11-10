import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-error-state',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './error-state.html',
  styleUrl: './error-state.scss',
})
export class ErrorStateComponent {
  readonly title = input<string>('Unable to complete request');
  readonly message = input<string>('An unexpected error occurred while loading data.');
  readonly correlationId = input<string>('');
  readonly retryLabel = input<string>('Retry');
  readonly retry = output<void>();
}
