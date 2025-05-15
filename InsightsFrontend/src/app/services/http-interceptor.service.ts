import { Injectable } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class HttpInterceptorService implements HttpInterceptor {
	intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
		const requestWithHeaders = request.clone({
			setHeaders: {
				'Content-Type': 'application/json',
			},
		});

		return next.handle(requestWithHeaders);
	}
}
