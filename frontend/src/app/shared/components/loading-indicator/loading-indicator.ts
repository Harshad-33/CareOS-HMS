import { Component, input } from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-indicator',
  standalone: true,
  imports: [MatProgressBarModule, MatProgressSpinnerModule],
  templateUrl: './loading-indicator.html',
  styleUrl: './loading-indicator.scss',
})
export class LoadingIndicatorComponent {
  readonly mode = input<'inline' | 'bar'>('inline');
  readonly message = input<string>('Loading...');
  readonly diameter = input<number>(32);
}
