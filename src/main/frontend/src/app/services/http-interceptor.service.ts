import { Injectable } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

/**
 * Union type of all possible HTTP request/response body types in the application
 */
export type HttpBody = object | null | void;

@Injectable()
export class HttpInterceptorService implements HttpInterceptor {
  intercept(request: HttpRequest<HttpBody>, next: HttpHandler): Observable<HttpEvent<HttpBody>> {
    // Get CSRF token from cookie
    const csrfToken = this.getCsrfToken();

    console.log('[HTTP Interceptor] Request:', {
      method: request.method,
      url: request.url,
      csrfTokenFound: !!csrfToken,
      csrfToken: csrfToken ? `${csrfToken.substring(0, 10)}...` : 'none',
      allCookies: document.cookie,
      withCredentials: true,
    });

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Add CSRF token header for state-changing requests (POST, PUT, DELETE, PATCH)
    if (csrfToken && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(request.method)) {
      headers['X-XSRF-TOKEN'] = csrfToken;
      console.log('[HTTP Interceptor] Adding X-XSRF-TOKEN header for', request.method, 'request');
    } else if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(request.method)) {
      console.warn('[HTTP Interceptor] WARNING: No CSRF token found for', request.method, 'request!');
    }

    const requestWithHeaders = request.clone({
      setHeaders: headers,
      withCredentials: true,
    });

    console.log('[HTTP Interceptor] Final headers:', requestWithHeaders.headers.keys());

    return next.handle(requestWithHeaders).pipe(
      tap({
        next: (event) => {
          if (event instanceof HttpResponse) {
            console.log('[HTTP Interceptor] Success:', {
              method: request.method,
              url: request.url,
              status: event.status,
              statusText: event.statusText,
            });
          }
        },
        error: (error: HttpErrorResponse) => {
          console.error('[HTTP Interceptor] Error:', {
            method: request.method,
            url: request.url,
            status: error.status,
            statusText: error.statusText,
            message: error.message,
            error: error.error,
          });
        },
      })
    );
  }

  private getCsrfToken(): string | null {
    const name = 'XSRF-TOKEN=';
    const decodedCookie = decodeURIComponent(document.cookie);
    const cookies = decodedCookie.split(';');

    for (const cookie of cookies) {
      const trimmedCookie = cookie.trim();
      if (trimmedCookie.startsWith(name)) {
        return trimmedCookie.substring(name.length);
      }
    }

    return null;
  }
}
