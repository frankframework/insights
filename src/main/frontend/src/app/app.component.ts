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
import { FeedbackComponent } from './components/feedback/feedback.component';
import { HeaderComponent } from './pages/header/header.component';
import { TooltipComponent } from './pages/release-roadmap/issue-bar/tooltip/tooltip.component';
import { AuthService } from './services/auth.service';
import { GraphStateService } from './services/graph-state.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoaderComponent, FeedbackComponent, HeaderComponent, TooltipComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  title = 'FF! Insights';

  public loading = false;

  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private graphStateService = inject(GraphStateService);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) this.loading = true;
      if (event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError)
        this.loading = false;
    });
  }

  ngOnInit(): void {
    const wasExtended = this.graphStateService.restoreAndClearOAuthExtended();

    this.authService.checkAuthStatus().subscribe({
      next: (user) => {
        if (user) {
          this.authService.setAuthenticated(user);
        }
      },
    });

    this.route.queryParams.subscribe((parameters) => {
      const currentUrl = this.router.url;
      const isGraphRoute = currentUrl.startsWith('/graph') || currentUrl === '/';

      if (wasExtended && isGraphRoute && !currentUrl.includes('extended')) {
        this.graphStateService.setShowExtendedSupport(true);
        this.router.navigate([], {
          queryParams: { extended: '' },
          replaceUrl: true,
        });
        return;
      }

      if (isGraphRoute) {
        if (parameters['extended'] === undefined) {
          this.graphStateService.setShowExtendedSupport(false);
        } else {
          this.graphStateService.setShowExtendedSupport(true);
        }
      }
    });
  }
}
