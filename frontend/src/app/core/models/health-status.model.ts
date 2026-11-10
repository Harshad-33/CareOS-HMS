export interface HealthStatus {
  status: 'UP' | 'DOWN' | string;
  service: string;
  version: string;
  database: 'UP' | 'DOWN' | string;
  timestamp: string;
}
