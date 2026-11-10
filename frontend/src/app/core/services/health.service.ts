import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HealthStatus } from '../models/health-status.model';
import { ApiClientService } from './api-client.service';

@Injectable({
  providedIn: 'root',
})
export class HealthService {
  private readonly apiClient = inject(ApiClientService);

  getHealth(): Observable<HealthStatus> {
    return this.apiClient.get<HealthStatus>('/health');
  }
}
