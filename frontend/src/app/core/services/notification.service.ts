import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { APP_CONSTANTS } from '../constants/app.constants';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  info(message: string): void {
    this.snackBar.open(message, 'Dismiss', {
      duration: APP_CONSTANTS.defaultSnackbarDurationMs,
      panelClass: ['careos-snackbar-info'],
    });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Close', {
      duration: APP_CONSTANTS.defaultSnackbarDurationMs + 1500,
      panelClass: ['careos-snackbar-error'],
    });
  }
}
