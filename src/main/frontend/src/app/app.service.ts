import { Injectable } from '@angular/core';

export const GitHubStates = {
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
} as const;

export type GitHubState = (typeof GitHubStates)[keyof typeof GitHubStates];

@Injectable({
  providedIn: 'root',
})
export class AppService {
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
