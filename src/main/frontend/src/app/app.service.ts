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
  private readonly WITH_CREDENTIALS: string = 'withCredentials';
  private http = inject(HttpClient);

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
    const options: { params?: HttpParams; withCredentials?: boolean } = {};

    if (parameters) {
      for (const key of Object.keys(parameters)) {
        const value = parameters[key];

        if (key === this.WITH_CREDENTIALS) {
          options.withCredentials = value as boolean;
          continue;
        }

        if (value !== undefined && value !== null && value !== '') {
          httpParameters = httpParameters.set(key, String(value));
        }
      }
    }

    options.params = httpParameters;
    return this.http.get<T>(url, options);
  }

  /**
   * Performs a POST request to the given URL with optional body and options.
   *
   * @template T - The expected response type.
   * @template B - The request body type.
   * @param url - The API endpoint URL.
   * @param body - Optional request body.
   * @param options - Optional HTTP options like withCredentials.
   * @returns An Observable of type `T` containing the response data.
   */
  public post<T, B = unknown>(url: string, body?: B, options?: Record<string, boolean | string>): Observable<T> {
    const httpOptions: { withCredentials?: boolean; headers?: Record<string, string> } = {};

    if (options) {
      if (options[this.WITH_CREDENTIALS] !== undefined) {
        httpOptions.withCredentials = options[this.WITH_CREDENTIALS] as boolean;
      }

      const headerKeys = Object.keys(options).filter((key) => key !== this.WITH_CREDENTIALS);
      if (headerKeys.length > 0) {
        httpOptions.headers = {};
        for (const key of headerKeys) {
          const value = options[key];
          if (typeof value === 'string') {
            httpOptions.headers[key] = value;
          }
        }
      }
    }

    return this.http.post<T>(url, body ?? null, httpOptions);
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
