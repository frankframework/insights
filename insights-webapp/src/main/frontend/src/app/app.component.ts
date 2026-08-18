import { Component, OnInit, Signal, inject } from '@angular/core';
import { Location } from '@angular/common';
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
import { parseVersionRanges, serializeVersionRanges } from './pipes/release-range';

const isNavigationSettled = (event: RouterEvent): boolean =>
  event instanceof NavigationEnd || event instanceof NavigationCancel || event instanceof NavigationError;

const GRAPH_QUERY_PARAMETERS = new Set(['extended', 'nightly', 'range']);

const queryStringOf = (url: string): string => {
  const start = url.indexOf('?');
  return start === -1 ? '' : url.slice(start);
};

const isGraphPath = (url: string): boolean => {
  const path = url.split(/[#?]/)[0];
  return path === '' || path === '/' || path === '/graph' || path.startsWith('/graph/');
};

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
  private readonly location = inject(Location);
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
        this.canonicaliseGraphUrl();
      }
    });
  }

  private canonicaliseGraphUrl(): void {
    const browserUrl = this.location.path(true);
    if (!isGraphPath(browserUrl)) return;

    const tree = this.router.parseUrl(browserUrl);
    const parsedRanges = parseVersionRanges(tree.queryParams['range']);
    if (parsedRanges.error) return;

    const canonical: Record<string, string> = Object.fromEntries(
      Object.entries(tree.queryParams).filter(([name]) => !GRAPH_QUERY_PARAMETERS.has(name)),
    );

    const level = GraphStateService.parseExtendedSupportLevel(tree.queryParams['extended']);
    if (level > 0) canonical['extended'] = String(level);
    if ('nightly' in tree.queryParams) canonical['nightly'] = '';

    const range = serializeVersionRanges(parsedRanges.ranges);
    if (range) canonical['range'] = range;

    tree.queryParams = canonical;

    const canonicalUrl = this.router.serializeUrl(tree);
    if (queryStringOf(browserUrl) === queryStringOf(canonicalUrl)) return;

    this.router.navigateByUrl(canonicalUrl, { replaceUrl: true });
  }
}
