import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  Router,
  NavigationStart,
  NavigationEnd,
  NavigationCancel,
  NavigationError,
  ActivatedRoute,
} from '@angular/router';
import { Subject } from 'rxjs';
import { AppComponent } from './app.component';
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

@Component({ selector: 'router-outlet', template: '' })
class MockRouterOutletComponent {}

class MockRouter {
  public events = new Subject<NavigationStart | NavigationEnd | NavigationCancel | NavigationError>();
}

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let router: MockRouter;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, MockRouterOutletComponent],
      providers: [
        { provide: Router, useClass: MockRouter },
        { provide: ActivatedRoute, useValue: {} },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as unknown as MockRouter;
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it(`should have the title 'FF! Insights'`, () => {
    expect(component.title).toEqual('FF! Insights');
  });

  it('should have loading set to false initially', () => {
    expect(component.loading).toBe(false);
  });

  describe('Router Events Handling', () => {
    it('should set loading to true on NavigationStart event', () => {
      fixture.detectChanges();

      // Simulate the event
      router.events.next(new NavigationStart(1, '/new-page'));

      // Check the result
      expect(component.loading).toBe(true);
    });

    it('should set loading to false on NavigationEnd event', () => {
      component.loading = true;
      fixture.detectChanges();

      router.events.next(new NavigationEnd(1, '/new-page', '/new-page'));

      expect(component.loading).toBe(false);
    });

    it('should set loading to false on NavigationCancel event', () => {
      component.loading = true;
      fixture.detectChanges();

      router.events.next(new NavigationCancel(1, '/new-page', 'Guard returned false'));

      expect(component.loading).toBe(false);
    });

    it('should set loading to false on NavigationError event', () => {
      component.loading = true;
      fixture.detectChanges();

      router.events.next(new NavigationError(1, '/new-page', new Error('Route not found')));

      expect(component.loading).toBe(false);
    });

    it('should correctly handle a sequence of navigation events', () => {
      fixture.detectChanges();
      router.events.next(new NavigationStart(1, '/'));

      expect(component.loading).toBe(true);

      router.events.next(new NavigationEnd(1, '/', '/'));

      expect(component.loading).toBe(false);

      router.events.next(new NavigationStart(2, '/other'));

      expect(component.loading).toBe(true);
    });
  });
});
