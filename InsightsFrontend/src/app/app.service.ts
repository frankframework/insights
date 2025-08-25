import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export const GitHubStates = {
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
} as const;

export type GitHubState = (typeof GitHubStates)[keyof typeof GitHubStates];

@Injectable({
  providedIn: 'root',
})
export class AppService {
  private static readonly API_BASE_URL: string = environment.backendUrl;

  private http = inject(HttpClient);

  /**
   * Performs a GET request to the given URL with optional query parameters.
   *
   * @template T - The expected response type.
   * @param url - The API endpoint URL.
   * @param parameters - Optional query parameters as key-value pairs. Values must be primitive types.
   * @returns An Observable of type `T` containing the response data.
   */
  public get<T>(url: string, parameters?: Record<string, string | number>): Observable<T> {
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
   *   * Constructs a qualified API URL by appending a given endpoint to the base URL.
   *
   * @param endpoint The string that extends the base url to reach a specific endpoint
   * @returns The complete API URL.
   */
  public createAPIUrl(endpoint: string): string {
    return `${AppService.API_BASE_URL}/${endpoint}`;
  }
}
