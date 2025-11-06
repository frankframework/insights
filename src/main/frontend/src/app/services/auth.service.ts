import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';

export interface User {
  githubId: number;
  username: string;
  avatarUrl: string;
  isFrankframeworkMember: boolean;
}

export interface ErrorResponse {
  httpStatus: number;
  messages: string[];
  errorCode: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  public currentUser = signal<User | null>(null);
  public isAuthenticated = signal<boolean>(false);
  public authError = signal<string | null>(null);

  private http = inject(HttpClient);

  public checkAuthStatus(): Observable<User | null> {
    this.authError.set(null);

    return this.http.get<User>('/api/auth/user', { withCredentials: true }).pipe(
      tap((user) => {
        this.currentUser.set(user);
        this.isAuthenticated.set(true);
        this.authError.set(null);
      }),
      catchError((error: HttpErrorResponse) => {
        this.currentUser.set(null);
        this.isAuthenticated.set(false);

        if (error.status === 401 || error.status === 403) {
          const errorResponse = error.error as ErrorResponse;
          if (errorResponse?.messages?.length > 0) {
            this.authError.set(errorResponse.messages.join(' '));
          } else if (error.status === 403) {
            this.authError.set('Access denied. You must be a member of the frankframework organization.');
          } else if (error.status === 401) {
            this.authError.set(null);
          }
        }

        return of(null);
      }),
    );
  }

  /**
   * Public method to manually set the authentication error from outside.
   * Used by the header component to display errors from the popup.
   * @param message The error message to display.
   */
  public setAuthError(message: string | null): void {
    this.authError.set(message);
  }

  public clearError(): void {
    this.authError.set(null);
  }

  public logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
      tap(() => {
        this.currentUser.set(null);
        this.isAuthenticated.set(false);
        this.authError.set(null);
        globalThis.location.href = '/';
      }),
    );
  }
}
