import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
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
import { TooltipComponent } from './components/tooltip/tooltip.component';
import { AuthService } from './services/auth.service';
import { GraphStateService } from './services/graph-state.service';
import { parseVersionRanges, serializeVersionRanges } from './pipes/release-range';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LoaderComponent, FeedbackComponent, HeaderComponent, TooltipComponent],
  templateUrl: './app.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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
    const returnUrl = this.authService.consumeReturnUrl();
    const restoredExtendedSupportLevel = this.graphStateService.restoreAndClearOAuthExtendedSupportLevel();
    const restoredNightly = this.graphStateService.restoreAndClearOAuthNightly();
    const restoredPreviousRanges = this.graphStateService.restoreAndClearOAuthRange();

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

    this.route.queryParamMap.subscribe((parameters) => {
      const currentUrl = this.router.url;
      const isGraphRoute = currentUrl.startsWith('/graph') || currentUrl === '/';

      if ((restoredExtendedSupportLevel > 0 || restoredNightly || restoredPreviousRanges.length > 0) && isGraphRoute) {
        const queryParameters: Record<string, string> = {};
        if (restoredExtendedSupportLevel > 0) queryParameters['extended'] = String(restoredExtendedSupportLevel);
        if (restoredNightly) queryParameters['nightly'] = '';

        const range = serializeVersionRanges(restoredPreviousRanges);
        if (range) queryParameters['range'] = range;

        this.graphStateService.setExtendedSupportLevel(restoredExtendedSupportLevel);
        this.graphStateService.setShowNightlies(restoredNightly);
        this.graphStateService.setReleaseRanges(restoredPreviousRanges);
        this.router.navigate([], { queryParams: queryParameters, replaceUrl: true });
        return;
      }

      if (isGraphRoute) {
        this.graphStateService.setExtendedSupportLevel(
          GraphStateService.parseExtendedSupportLevel(parameters.get('extended')),
        );
        this.graphStateService.setShowNightlies(parameters.has('nightly'));
        this.graphStateService.setReleaseRanges(parseVersionRanges(parameters.get('range')).ranges);
      }
    });
  }
}
