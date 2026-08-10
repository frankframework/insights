import { Component, OnInit, Signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  RouterOutlet,
  Router,
  NavigationEnd,
  NavigationCancel,
  NavigationError,
  NavigationStart,
  ActivatedRoute,
  Event as RouterEvent,
} from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { LoaderComponent } from './components/loader/loader.component';
import { FeedbackComponent } from './components/feedback/feedback.component';
import { HeaderComponent } from './pages/header/header.component';
import { TooltipComponent } from './components/tooltip/tooltip.component';
import { AuthService } from './services/auth.service';
import { GraphStateService } from './services/graph-state.service';

const isNavigationSettled = (event: RouterEvent): boolean =>
  event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError;

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoaderComponent, FeedbackComponent, HeaderComponent, TooltipComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  public readonly title = 'FF! Insights';
  public readonly loading: Signal<boolean>;

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly graphStateService = inject(GraphStateService);

  constructor() {
    this.loading = toSignal(
      this.router.events.pipe(
        filter((event) => event instanceof NavigationStart || isNavigationSettled(event)),
        map((event) => event instanceof NavigationStart),
      ),
      { initialValue: false },
    );
  }

  ngOnInit(): void {
    const returnUrl = this.authService.consumeReturnUrl();
    const restoredExtendedSupportLevel = this.graphStateService.restoreAndClearOAuthExtendedSupportLevel();
    const restoredNightly = this.graphStateService.restoreAndClearOAuthNightly();

    this.authService.checkAuthStatus().subscribe({
      next: (user) => {
        if (user) {
          this.authService.setAuthenticated(user);
        }
      },
    });

    if (returnUrl) {
      this.router.navigateByUrl(returnUrl, { replaceUrl: true });
      return;
    }

    this.route.queryParams.subscribe((parameters) => {
      const currentUrl = this.router.url;
      const isGraphRoute = currentUrl.startsWith('/graph') || currentUrl === '/';

      if ((restoredExtendedSupportLevel > 0 || restoredNightly) && isGraphRoute) {
        const queryParameters: Record<string, string> = {};
        if (restoredExtendedSupportLevel > 0) queryParameters['extended'] = String(restoredExtendedSupportLevel);
        if (restoredNightly) queryParameters['nightly'] = '';

        this.graphStateService.setExtendedSupportLevel(restoredExtendedSupportLevel);
        this.graphStateService.setShowNightlies(restoredNightly);
        this.router.navigate([], { queryParams: queryParameters, replaceUrl: true });
        return;
      }

      if (isGraphRoute) {
        this.graphStateService.setExtendedSupportLevel(
          GraphStateService.parseExtendedSupportLevel(parameters['extended']),
        );
        this.graphStateService.setShowNightlies(parameters['nightly'] !== undefined);
      }
    });
  }
}
