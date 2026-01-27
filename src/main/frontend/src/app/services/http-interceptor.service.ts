import { Injectable, inject } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { AuthService } from './auth.service';

export type HttpBody = object | null | void;

@Injectable()
export class HttpInterceptorService implements HttpInterceptor {
  private readonly authService: AuthService = inject(AuthService);
  private readonly router: Router = inject(Router);

  private isLoggingOut = false;

  intercept(request: HttpRequest<HttpBody>, next: HttpHandler): Observable<HttpEvent<HttpBody>> {
    return next.handle(request.clone({ withCredentials: true })).pipe(
      catchError((error: HttpErrorResponse) => {
        this.handleError(request, error);
        return throwError(() => error);
      }),
    );
  }

  private handleError(request: HttpRequest<HttpBody>, error: HttpErrorResponse): void {
    console.error('Error:', {
      method: request.method,
      url: request.url,
      status: error.status,
      statusText: error.statusText,
      message: error.message,
      error: error.error,
    });

    if (this.shouldSkipLogout(request)) {
      return;
    }

    if (this.shouldLogout(error)) {
      console.warn(`Encountered ${error.status} error. Logging out...`);
      this.performLogout();
    }
  }

  private shouldSkipLogout(request: HttpRequest<HttpBody>): boolean {
    return this.isLoggingOut || request.url.includes('/auth/logout') || request.url.includes('/auth/user');
  }

  private shouldLogout(error: HttpErrorResponse): boolean {
    return error.status === 401 || error.status === 403;
  }

  private performLogout(): void {
    this.isLoggingOut = true;
    this.authService
      .logout()
      .pipe(
        finalize(() => {
          this.isLoggingOut = false;
          this.router.navigate(['/graph']);
        }),
      )
      .subscribe({
        error: (logoutError) => console.error('Error during logout:', logoutError),
      });
  }
}
