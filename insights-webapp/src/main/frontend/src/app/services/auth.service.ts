import { Injectable, Signal, WritableSignal, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { AppService } from '../app.service';
import { LocationService } from './location.service';

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
  public readonly currentUser: Signal<User | null>;
  public readonly isAuthenticated: Signal<boolean>;
  public readonly authError: Signal<string | null>;
  public readonly isLoading: Signal<boolean>;

  private readonly currentUserState: WritableSignal<User | null> = signal<User | null>(null);
  private readonly isAuthenticatedState: WritableSignal<boolean> = signal<boolean>(false);
  private readonly authErrorState: WritableSignal<string | null> = signal<string | null>(null);
  private readonly isLoadingState: WritableSignal<boolean> = signal<boolean>(false);

  private readonly SESSION_KEY: string = 'auth_session';
  private readonly RETURN_URL_KEY: string = 'auth_return_url';
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);
  private readonly locationService: LocationService = inject(LocationService);

  constructor() {
    this.currentUser = this.currentUserState.asReadonly();
    this.isAuthenticated = this.isAuthenticatedState.asReadonly();
    this.authError = this.authErrorState.asReadonly();
    this.isLoading = this.isLoadingState.asReadonly();
  }

  /**
   * Check authentication status by calling the backend
   * Returns user info if authenticated and authorized (frankframework member)
   * Sets appropriate error messages for 403 (not authorized), but not for 401 (just not logged in)
   */
  public checkAuthStatus(): Observable<User | null> {
    const hasSession = localStorage.getItem(this.SESSION_KEY) === 'true';

    if (!hasSession) {
      this.isAuthenticatedState.set(false);
      return of(null);
    }

    this.isLoadingState.set(true);
    return this.http.get<User>(this.appService.createAPIUrl('auth/user')).pipe(
      finalize(() => this.isLoadingState.set(false)),
      catchError((error: HttpErrorResponse) => {
        this.handleAuthError(error);
        return of(null);
      }),
    );
  }

  public setAuthenticated(user: User): void {
    this.currentUserState.set(user);
    this.isAuthenticatedState.set(true);
    this.authErrorState.set(null);
    this.setSessionFlag(true);
  }

  public clearError(): void {
    this.authErrorState.set(null);
  }

  public setLoading(loading: boolean): void {
    this.isLoadingState.set(loading);
  }

  /**
   * Set session flag to indicate OAuth flow is in progress
   * This ensures checkAuthStatus will query the backend after OAuth redirect
   */
  public setPendingAuth(): void {
    this.setSessionFlag(true);
  }

  /**
   * Remember the in-app URL the user started the login from, so they can be returned
   * there after the OAuth round-trip (the backend otherwise lands on the default page).
   */
  public saveReturnUrl(url: string): void {
    localStorage.setItem(this.RETURN_URL_KEY, url);
  }

  /**
   * Read and clear the saved return URL. Returns null when no login redirect is pending.
   */
  public consumeReturnUrl(): string | null {
    const url = localStorage.getItem(this.RETURN_URL_KEY);
    localStorage.removeItem(this.RETURN_URL_KEY);
    return url;
  }

  public logout(): Observable<ArrayBuffer> {
    this.isLoadingState.set(true);
    return this.http.post<ArrayBuffer>(this.appService.createAPIUrl('auth/logout'), null).pipe(
      finalize(() => {
        this.isLoadingState.set(false);
        this.clearAuthState();
      }),
      catchError((error) => {
        console.error('AuthService: Logout failed, clearing state anyway:', error);
        return of();
      }),
    );
  }

  private clearAuthState(): void {
    this.currentUserState.set(null);
    this.isAuthenticatedState.set(false);
    this.authErrorState.set(null);
    this.setSessionFlag(false);

    this.locationService.navigateTo(globalThis.location.pathname + globalThis.location.search);
  }

  /**
   * Handle authentication errors from /api/auth/user
   * @param error The HTTP error response
   */
  private handleAuthError(error: HttpErrorResponse): void {
    this.currentUserState.set(null);
    this.isAuthenticatedState.set(false);
    this.setSessionFlag(false);

    if (error.status === 401) {
      console.log('AuthService: User is not authenticated (no active session)');
      this.authErrorState.set(null);
    } else if (error.status === 403) {
      console.warn('AuthService: User authenticated but not authorized (not a frankframework member)');
      const errorResponse = error.error as ErrorResponse;
      const message =
        errorResponse?.messages?.length > 0
          ? errorResponse.messages.join(' ')
          : 'Access denied. You must be a member of the frankframework organization.';
      this.authErrorState.set(message);
    } else {
      console.error('AuthService: Unexpected error from /api/auth/user:', error.status, error.message);
      this.authErrorState.set(null);
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
