import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RequestOptions {
  params?: Record<string, string | number | boolean | null | undefined>;
  withCredentials?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class ApiClientService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  get<T>(endpoint: string, options?: RequestOptions): Observable<T> {
    return this.http.get<T>(this.buildUrl(endpoint), {
      params: this.toHttpParams(options?.params),
      withCredentials: options?.withCredentials,
    });
  }

  post<T, B = unknown>(endpoint: string, body: B, options?: RequestOptions): Observable<T> {
    return this.http.post<T>(this.buildUrl(endpoint), body, {
      params: this.toHttpParams(options?.params),
      withCredentials: options?.withCredentials,
    });
  }

  put<T, B = unknown>(endpoint: string, body: B, options?: RequestOptions): Observable<T> {
    return this.http.put<T>(this.buildUrl(endpoint), body, {
      params: this.toHttpParams(options?.params),
      withCredentials: options?.withCredentials,
    });
  }

  patch<T, B = unknown>(endpoint: string, body: B, options?: RequestOptions): Observable<T> {
    return this.http.patch<T>(this.buildUrl(endpoint), body, {
      params: this.toHttpParams(options?.params),
      withCredentials: options?.withCredentials,
    });
  }

  delete<T>(endpoint: string, options?: RequestOptions): Observable<T> {
    return this.http.delete<T>(this.buildUrl(endpoint), {
      params: this.toHttpParams(options?.params),
      withCredentials: options?.withCredentials,
    });
  }

  private buildUrl(endpoint: string): string {
    const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    return `${this.baseUrl}${normalizedEndpoint}`;
  }

  private toHttpParams(
    params?: Record<string, string | number | boolean | null | undefined>
  ): HttpParams {
    let httpParams = new HttpParams();
    if (!params) {
      return httpParams;
    }

    for (const [key, value] of Object.entries(params)) {
      if (value !== null && value !== undefined && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    }
    return httpParams;
  }
}
