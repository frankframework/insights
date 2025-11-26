import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export const GitHubStates = {
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
} as const;

export type GitHubState = (typeof GitHubStates)[keyof typeof GitHubStates];

@Injectable({
  providedIn: 'root',
})
export class AppService {
  private http = inject(HttpClient);

  /**
   * Fetches the CSRF token from the backend to ensure the XSRF-TOKEN cookie is set.
   * This should be called on app initialization.
   */
  public initializeCsrfToken(): Observable<void> {
    return this.http.get<void>(this.createAPIUrl('csrf'));
  }

  /**
   * Performs a GET request to the given URL with optional query parameters.
   *
   * @template T - The expected response type.
   * @param url - The API endpoint URL.
   * @param parameters - Optional query parameters as key-value pairs. Values can be primitive types or HTTP options.
   * @returns An Observable of type `T` containing the response data.
   */
  public get<T>(url: string, parameters?: Record<string, string | number | boolean>): Observable<T> {
    let httpParameters = new HttpParams();

    if (parameters) {
      for (const key of Object.keys(parameters)) {
        const value = parameters[key];

        if (value !== undefined && value !== null && value !== '') {
          httpParameters = httpParameters.set(key, String(value));
        }
      }
    }

    return this.http.get<T>(url, { params: httpParameters });
  }

  /**
   * Performs a POST request to the given URL with optional body and options.
   *
   * @template T - The expected response type.
   * @template B - The request body type.
   * @param url - The API endpoint URL.
   * @param body - Optional request body.
   * @param options - Optional HTTP headers as key-value pairs.
   * @returns An Observable of type `T` containing the response data.
   */
  public post<T, B = unknown>(url: string, body?: B, options?: Record<string, string>): Observable<T> {
    const httpOptions: { headers?: Record<string, string> } = {};

    if (options && Object.keys(options).length > 0) {
      httpOptions.headers = options;
    }

    return this.http.post<T>(url, body ?? null, httpOptions);
  }

  /**
   * Performs a PUT request to the given URL with optional body and options.
   *
   * @template T - The expected response type.
   * @template B - The request body type.
   * @param url - The API endpoint URL.
   * @param body - Optional request body.
   * @param options - Optional HTTP headers as key-value pairs.
   * @returns An Observable of type `T` containing the response data.
   */
  public put<T, B = unknown>(url: string, body?: B, options?: Record<string, string>): Observable<T> {
    const httpOptions: { headers?: Record<string, string> } = {};

    if (options && Object.keys(options).length > 0) {
      httpOptions.headers = options;
    }

    return this.http.put<T>(url, body ?? null, httpOptions);
  }

  public delete<T>(url: string, options?: Record<string, string>): Observable<T> {
    const httpOptions: { headers?: Record<string, string> } = {};

    if (options && Object.keys(options).length > 0) {
      httpOptions.headers = options;
    }

    return this.http.delete<T>(url, httpOptions);
  }

  /**
   * Constructs a qualified API URL by appending a given endpoint to the base URL.
   *
   * @param endpoint The string that extends the base url to reach a specific endpoint
   * @returns The complete API URL.
   */
  public createAPIUrl(endpoint: string): string {
    return `/api/${endpoint}`;
  }
}
