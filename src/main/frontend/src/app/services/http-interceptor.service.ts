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
        },
      }),
    );
  }
}
