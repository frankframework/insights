import { Component, inject, OnInit } from '@angular/core';
import {
  RouterOutlet,
  Router,
  NavigationEnd,
  NavigationCancel,
  NavigationError,
  NavigationStart,
  ActivatedRoute,
} from '@angular/router';
import { LoaderComponent } from './components/loader/loader.component';
import { HeaderComponent } from './pages/header/header.component';
import { TooltipComponent } from './pages/release-roadmap/issue-bar/tooltip/tooltip.component';
import { AuthService } from './services/auth.service';
import { AppService } from './app.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoaderComponent, HeaderComponent, TooltipComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  title = 'FF! Insights';

  public loading = false;

  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private appService = inject(AppService);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) this.loading = true;
      if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError)
        this.loading = false;
    });
  }

  ngOnInit(): void {
    this.appService.initializeCsrfToken().subscribe({
      error: (error) => console.error('Failed to initialize CSRF token:', error),
    });

    if (this.authService.hasSessionFlag()) {
      this.authService.checkAuthStatus().subscribe();
    }

    this.route.queryParams.subscribe((parameters) => {
      const loginParameter = parameters['login'];

      if (loginParameter === 'success') {
        this.authService.checkAuthStatus().subscribe({
          next: () => {
            this.router.navigate([], {
              queryParams: {},
              replaceUrl: true,
            });
          },
          error: (error) => {
            console.error('Failed to fetch user info after OAuth success:', error);
            this.authService.setLoading(false);
            this.router.navigate([], {
              queryParams: {},
              replaceUrl: true,
            });
          },
        });
      } else if (loginParameter === 'error') {
        console.error('OAuth2 login failed');
        this.authService.setLoading(false);
        this.authService.clearError();
        this.router.navigate([], {
          queryParams: {},
          replaceUrl: true,
        });
      }
    });
  }
}
