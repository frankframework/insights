import { Injectable, inject } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthService } from './auth.service';

/**
 * Union type of all possible HTTP request/response body types in the application
 */
export type HttpBody = object | null | void;

@Injectable()
export class HttpInterceptorService implements HttpInterceptor {
  private authService = inject(AuthService);
  private router = inject(Router);

  intercept(request: HttpRequest<HttpBody>, next: HttpHandler): Observable<HttpEvent<HttpBody>> {
    const requestWithHeaders = request.clone({
      setHeaders: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });

    return next.handle(requestWithHeaders).pipe(
      tap({
        error: (error: HttpErrorResponse) => {
          console.error('Error:', {
            method: request.method,
            url: request.url,
            status: error.status,
            statusText: error.statusText,
            message: error.message,
            error: error.error,
          });

          if (this.shouldLogout(error)) {
            console.warn(`Encountered ${error.status} error. Logging out...`);
            this.authService.logout().subscribe({
              next: () => {
                this.router.navigate(['/graph']);
              },
              error: (logoutError) => {
                console.error('Error during logout:', logoutError);
                this.router.navigate(['/graph']);
              },
            });
          }
        },
      }),
    );
  }

  private shouldLogout(error: HttpErrorResponse): boolean {
    return error.status === 401 || error.status === 403 || error.status === 429 || error.status === 500;
  }
}
