import { Injectable } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

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

    return next.handle(requestWithHeaders);
  }
}
