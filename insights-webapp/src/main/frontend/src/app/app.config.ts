import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, UrlSerializer, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { ReadableUrlSerializer } from './services/readable-url.serializer';
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
  withXsrfConfiguration,
  withXhr,
} from '@angular/common/http';
import { HttpInterceptorService } from './services/http-interceptor.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    { provide: UrlSerializer, useClass: ReadableUrlSerializer },
    provideHttpClient(
      withXhr(),
      withInterceptorsFromDi(),
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      }),
    ),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HttpInterceptorService,
      multi: true,
    },
  ],
};
