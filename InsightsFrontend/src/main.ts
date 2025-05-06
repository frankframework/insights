import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import {HTTP_INTERCEPTORS, provideHttpClient} from '@angular/common/http';
import {HttpInterceptorService} from "./app/services/http-interceptor.service";

bootstrapApplication(AppComponent, {
	providers: [
		provideRouter(routes),
		provideAnimations(),
		provideHttpClient(),
		{
			provide: HTTP_INTERCEPTORS,
			useClass: HttpInterceptorService,
			multi: true
		}
	]
}).catch(err => console.error(err));
