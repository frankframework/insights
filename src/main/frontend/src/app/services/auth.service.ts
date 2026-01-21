import { Injectable, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { AppService } from '../app.service';
import { LocationService } from './location.service';
import { GraphStateService } from './graph-state.service';

export interface User {
  githubId: number;
  username: string;
  avatarUrl: string;
  isFrankFrameworkMember: boolean;
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
  public isLoading = signal<boolean>(false);

  private readonly SESSION_KEY = 'auth_session';
  private appService = inject(AppService);
  private locationService = inject(LocationService);
  private graphStateService = inject(GraphStateService);

  /**
   * Check authentication status by calling the backend
   * Returns user info if authenticated and authorized (frankframework member)
   * Sets appropriate error messages for 403 (not authorized), but not for 401 (just not logged in)
   */
  public checkAuthStatus(): Observable<User | null> {
    const hasSession = localStorage.getItem(this.SESSION_KEY) === 'true';

    if (!hasSession) {
      this.isAuthenticated.set(false);
      return of(null);
    }

    this.isLoading.set(true);
    return this.appService.get<User>(this.appService.createAPIUrl('auth/user')).pipe(
      tap((user) => {
        this.currentUser.set(user);
        this.isAuthenticated.set(true);
        this.authError.set(null);
        this.isLoading.set(false);
        this.setSessionFlag(true);
      }),
      catchError((error: HttpErrorResponse) => {
        this.currentUser.set(null);
        this.isAuthenticated.set(false);
        this.isLoading.set(false);
        this.setSessionFlag(false);

        if (error.status === 401) {
          console.log('AuthService: User is not authenticated (no active session)');
          this.authError.set(null);
        } else if (error.status === 403) {
          console.warn('AuthService: User authenticated but not authorized (not a frankframework member)');
          const errorResponse = error.error as ErrorResponse;

          if (errorResponse?.messages?.length > 0) {
            this.authError.set(errorResponse.messages.join(' '));
          } else {
            this.authError.set('Access denied. You must be a member of the frankframework organization.');
          }
        } else {
          console.error('AuthService: Unexpected error from /api/auth/user:', error.status, error.message);
          this.authError.set(null);
        }

        return of(null);
      }),
    );
  }

  public clearError(): void {
    this.authError.set(null);
  }

  public setLoading(loading: boolean): void {
    this.isLoading.set(loading);
  }

  /**
   * Set session flag to indicate OAuth flow is in progress
   * This ensures checkAuthStatus will query the backend after OAuth redirect
   */
  public setPendingAuth(): void {
    this.setSessionFlag(true);
  }

  public logout(): Observable<void> {
    this.isLoading.set(true);
    return this.appService.post<void>(this.appService.createAPIUrl('auth/logout')).pipe(
      tap(() => {
        this.isLoading.set(false);
        this.clearAuthState();
      }),
      catchError((error) => {
        console.error('AuthService: Logout failed, clearing state anyway:', error);
        this.isLoading.set(false);
        this.clearAuthState();
        return of();
      }),
    );
  }

  private clearAuthState(): void {
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.authError.set(null);
    this.setSessionFlag(false);

    const queryParameters = this.graphStateService.getGraphQueryParams();
    const queryString =
      Object.keys(queryParameters).length > 0 ? `?${new URLSearchParams(queryParameters).toString()}` : '';
    this.locationService.navigateTo(`/${queryString}`);
  }

  /**
   * Set or clear the session flag in localStorage
   */
  private setSessionFlag(hasSession: boolean): void {
    if (hasSession) {
      localStorage.setItem(this.SESSION_KEY, 'true');
    } else {
      localStorage.removeItem(this.SESSION_KEY);
    }
  }
}
