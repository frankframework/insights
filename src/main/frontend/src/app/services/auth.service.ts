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

  /**
   * Check authentication status by calling the backend
   * Returns user info if authenticated and authorized (frankframework member)
   * Sets appropriate error messages for 401 (not authenticated) and 403 (not authorized)
   */
  public checkAuthStatus(): Observable<User | null> {
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
          } else {
            if (error.status === 401) {
              this.authError.set('You are not logged in. Please sign in with GitHub.');
            } else {
              this.authError.set('Access denied. You must be a member of the frankframework organization.');
            }
          }
        }

        return of(null);
      }),
    );
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
