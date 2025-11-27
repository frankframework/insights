import { Injectable } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

/**
 * Union type of all possible HTTP request/response body types in the application
 */
export type HttpBody = object | null | void;

@Injectable()
export class HttpInterceptorService implements HttpInterceptor {
  intercept(request: HttpRequest<HttpBody>, next: HttpHandler): Observable<HttpEvent<HttpBody>> {
    const csrfToken = this.getCsrfToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (csrfToken && ['POST', 'PUT', 'DELETE'].includes(request.method)) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    } else if (['POST', 'PUT', 'DELETE'].includes(request.method)) {
      console.warn('Warning: No CSRF token found for', request.method, 'request!');
    }

    const requestWithHeaders = request.clone({
      setHeaders: headers,
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
        },
      }),
    );
  }

  private getCsrfToken(): string | null {
    const name = 'XSRF-TOKEN=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const cookies = decodedCookie.split(';');

    for (const cookie of cookies) {
      const trimmedCookie = cookie.trim();
      if (trimmedCookie.startsWith(name)) {
        return trimmedCookie.slice(name.length);
      }
    }

    return null;
  }
}
