import { Injectable, inject, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { finalize } from 'rxjs/operators';
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
  public currentUser: WritableSignal<User | null> = signal<User | null>(null);
  public isAuthenticated: WritableSignal<boolean> = signal<boolean>(false);
  public authError: WritableSignal<string | null> = signal<string | null>(null);
  public isLoading: WritableSignal<boolean> = signal<boolean>(false);

  private readonly SESSION_KEY: string = 'auth_session';
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);
  private readonly locationService: LocationService = inject(LocationService);
  private readonly graphStateService: GraphStateService = inject(GraphStateService);

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
    return this.http.get<User>(this.appService.createAPIUrl('auth/user')).pipe(
      finalize(() => this.isLoading.set(false)),
      catchError((error: HttpErrorResponse) => {
        this.handleAuthError(error);
        return of(null);
      }),
    );
  }

  public setAuthenticated(user: User): void {
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
    this.authError.set(null);
    this.setSessionFlag(true);
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

  public logout(): Observable<ArrayBuffer> {
    this.isLoading.set(true);
    return this.http.post<ArrayBuffer>(this.appService.createAPIUrl('auth/logout'), null).pipe(
      finalize(() => {
        this.isLoading.set(false);
        this.clearAuthState();
      }),
      catchError((error) => {
        console.error('AuthService: Logout failed, clearing state anyway:', error);
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
   * Handle authentication errors from /api/auth/user
   * @param error The HTTP error response
   */
  private handleAuthError(error: HttpErrorResponse): void {
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.setSessionFlag(false);

    if (error.status === 401) {
      console.log('AuthService: User is not authenticated (no active session)');
      this.authError.set(null);
    } else if (error.status === 403) {
      console.warn('AuthService: User authenticated but not authorized (not a frankframework member)');
      const errorResponse = error.error as ErrorResponse;
      const message =
        errorResponse?.messages?.length > 0
          ? errorResponse.messages.join(' ')
          : 'Access denied. You must be a member of the frankframework organization.';
      this.authError.set(message);
    } else {
      console.error('AuthService: Unexpected error from /api/auth/user:', error.status, error.message);
      this.authError.set(null);
    }
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
