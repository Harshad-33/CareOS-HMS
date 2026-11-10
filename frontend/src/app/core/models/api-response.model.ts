export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  path: string;
  correlationId?: string;
}

export interface ApiErrorResponse {
  success: false;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  correlationId?: string;
  timestamp: string;
  fieldErrors?: ValidationFieldError[];
}

export interface ValidationFieldError {
  field: string;
  message: string;
}
