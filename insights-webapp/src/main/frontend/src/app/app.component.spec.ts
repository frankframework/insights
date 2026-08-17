import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  Router,
  NavigationStart,
  NavigationEnd,
  NavigationCancel,
  NavigationError,
  ActivatedRoute,
  convertToParamMap,
  UrlTree,
} from '@angular/router';
import { Subject, of } from 'rxjs';
import { AppComponent } from './app.component';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { Location } from '@angular/common';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReadableUrlSerializer } from './services/readable-url.serializer';

@Component({ selector: 'router-outlet', template: '' })
class MockRouterOutletComponent {}

const serializer = new ReadableUrlSerializer();

class MockRouter {
  public events = new Subject<NavigationStart | NavigationEnd | NavigationCancel | NavigationError>();
  public url = '/';
  navigate: jasmine.Spy | undefined;
  navigateByUrl: jasmine.Spy | undefined;

  parseUrl(url: string): UrlTree {
    return serializer.parse(url);
  }

  serializeUrl(tree: UrlTree): string {
    return serializer.serialize(tree);
  }
}

class MockActivatedRoute {
  queryParamMap = of(convertToParamMap({}));
}

class MockLocation {
  public currentPath = '/';
  replaceState: jasmine.Spy | undefined;

  path(): string {
    return this.currentPath;
  }
}

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let router: MockRouter;
  let location: MockLocation;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, MockRouterOutletComponent],
      providers: [
        { provide: Router, useClass: MockRouter },
        { provide: ActivatedRoute, useClass: MockActivatedRoute },
        { provide: Location, useClass: MockLocation },
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as unknown as MockRouter;
    location = TestBed.inject(Location) as unknown as MockLocation;
    router.navigate = jasmine.createSpy('navigate').and.resolveTo(true);
    router.navigateByUrl = jasmine.createSpy('navigateByUrl').and.resolveTo(true);
    location.replaceState = jasmine.createSpy('replaceState');
    // eslint-disable-next-line no-undef
    localStorage.removeItem('auth_return_url');
  });

  const open = (addressBar: string, routerUrl: string, parameters: Record<string, string>): void => {
    location.currentPath = addressBar;
    router.url = routerUrl;
    (TestBed.inject(ActivatedRoute) as unknown as MockActivatedRoute).queryParamMap = of(convertToParamMap(parameters));

    fixture.detectChanges();
  };

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it(`should have the title 'FF! Insights'`, () => {
    expect(component.title).toEqual('FF! Insights');
  });

  it('should have loading set to false initially', () => {
    expect(component.loading()).toBe(false);
  });

  describe('OAuth return URL handling', () => {
    it('redirects to the saved return URL on init and clears it', () => {
      // eslint-disable-next-line no-undef
      localStorage.setItem('auth_return_url', '/cve-overview');

      fixture.detectChanges();

      expect(router.navigateByUrl).toHaveBeenCalledWith('/cve-overview', { replaceUrl: true });
      // eslint-disable-next-line no-undef
      expect(localStorage.getItem('auth_return_url')).toBeNull();
    });

    it('does not redirect when no return URL is saved', () => {
      fixture.detectChanges();

      expect(router.navigateByUrl).not.toHaveBeenCalled();
    });
  });

  describe('Graph URL canonicalisation', () => {
    it('rewrites a link shared with percent-encoded brackets', () => {
      open('/graph?range=%5B9.0%5D,%5B9.4%5D', '/graph?range=[9.0],[9.4]', { range: '[9.0],[9.4]' });

      expect(location.replaceState).toHaveBeenCalledWith('/graph?range=[9.0],[9.4]');
    });

    it('leaves an already canonical url alone', () => {
      open('/graph?range=[9.0],[9.4]', '/graph?range=[9.0],[9.4]', { range: '[9.0],[9.4]' });

      expect(location.replaceState).not.toHaveBeenCalled();
    });

    it('collapses adjacent ranges into the merged range the graph actually shows', () => {
      open('/graph?range=[9.0,9.1),[9.1,9.2)', '/graph?range=[9.0,9.1),[9.1,9.2)', { range: '[9.0,9.1),[9.1,9.2)' });

      expect(location.replaceState).toHaveBeenCalledWith('/graph?range=[9.0,9.2)');
    });

    it('normalises a v prefix and stray whitespace', () => {
      open('/graph?range=%20v9.0%20', '/graph?range= v9.0 ', { range: ' v9.0 ' });

      expect(location.replaceState).toHaveBeenCalledWith('/graph?range=[9.0,)');
    });

    it('clamps an out of bounds extended support level', () => {
      open('/graph?extended=99', '/graph?extended=99', { extended: '99' });

      expect(location.replaceState).toHaveBeenCalledWith('/graph?extended=3');
    });

    it('drops an extended level that resolves to nothing', () => {
      open('/graph?extended=0', '/graph?extended=0', { extended: '0' });

      expect(location.replaceState).toHaveBeenCalledWith('/graph');
    });

    it('keeps query parameters it does not manage', () => {
      open('/graph?range=%5B9.0%5D&ref=slack', '/graph?range=[9.0]&ref=slack', { range: '[9.0]', ref: 'slack' });

      expect(location.replaceState).toHaveBeenCalledWith('/graph?ref=slack&range=[9.0]');
    });

    it('preserves the release tag on a deep graph link', () => {
      open('/graph/v9.0.0?range=%5B9.0%5D', '/graph/v9.0.0?range=[9.0]', { range: '[9.0]' });

      expect(location.replaceState).toHaveBeenCalledWith('/graph/v9.0.0?range=[9.0]');
    });

    it('leaves the url alone while the router has not caught up with the browser yet', () => {
      open('/graph?range=[9.0]', '/', {});

      expect(location.replaceState).not.toHaveBeenCalled();
    });

    it('keeps a range it cannot parse so it stays visible and correctable', () => {
      open('/graph?range=[9.0', '/graph?range=[9.0', { range: '[9.0' });

      expect(location.replaceState).not.toHaveBeenCalled();
    });

    it('does not touch a route outside the graph', () => {
      open('/cve-overview?range=%5B9.0%5D', '/cve-overview?range=[9.0]', { range: '[9.0]' });

      expect(location.replaceState).not.toHaveBeenCalled();
    });
  });

  describe('Router Events Handling', () => {
    it('should set loading to true on NavigationStart event', () => {
      fixture.detectChanges();

      router.events.next(new NavigationStart(1, '/new-page'));

      expect(component.loading()).toBe(true);
    });

    it('should set loading to false on NavigationEnd event', () => {
      fixture.detectChanges();
      router.events.next(new NavigationStart(1, '/new-page'));

      router.events.next(new NavigationEnd(1, '/new-page', '/new-page'));

      expect(component.loading()).toBe(false);
    });

    it('should set loading to false on NavigationCancel event', () => {
      fixture.detectChanges();
      router.events.next(new NavigationStart(1, '/new-page'));

      router.events.next(new NavigationCancel(1, '/new-page', 'Guard returned false'));

      expect(component.loading()).toBe(false);
    });

    it('should set loading to false on NavigationError event', () => {
      fixture.detectChanges();
      router.events.next(new NavigationStart(1, '/new-page'));

      router.events.next(new NavigationError(1, '/new-page', new Error('Route not found')));

      expect(component.loading()).toBe(false);
    });

    it('should correctly handle a sequence of navigation events', () => {
      fixture.detectChanges();
      router.events.next(new NavigationStart(1, '/'));

      expect(component.loading()).toBe(true);

      router.events.next(new NavigationEnd(1, '/', '/'));

      expect(component.loading()).toBe(false);

      router.events.next(new NavigationStart(2, '/other'));

      expect(component.loading()).toBe(true);
    });
  });
});
