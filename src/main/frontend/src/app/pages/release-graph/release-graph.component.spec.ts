import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReleaseGraphComponent } from './release-graph.component';
import { ReleaseService, Release } from '../../services/release.service';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLinkService } from './release-link.service';
import { Router, NavigationEnd } from '@angular/router';
import { of, ReplaySubject, throwError } from 'rxjs';
import { ElementRef } from '@angular/core';

describe('ReleaseGraphComponent', () => {
  let component: ReleaseGraphComponent;
  let fixture: ComponentFixture<ReleaseGraphComponent>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockNodeService: jasmine.SpyObj<ReleaseNodeService>;
  let mockLinkService: jasmine.SpyObj<ReleaseLinkService>;
  let routerEventsSubject: ReplaySubject<NavigationEnd>;

  const mockReleases: Release[] = [
    { id: '1', name: 'v1.0.0', tagName: 'v1', publishedAt: new Date(), branch: { id: 'b1', name: 'master' } },
  ];
  const mockNodes: ReleaseNode[] = [
    {
      id: '1',
      label: 'v1.0.0',
      position: { x: 100, y: 50 },
      branch: 'master',
      color: 'green',
      publishedAt: new Date(),
    },
  ];
  const mockLinks = [{ id: '1-2', source: '1', target: '2' }];
  const mockStructuredGroups = [new Map([['master', mockNodes]])];
  const mockReleaseNodeMap = new Map([['master', mockNodes]]);

  beforeEach(async () => {
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getAllReleases']);
    mockNodeService = jasmine.createSpyObj('ReleaseNodeService', [
      'structureReleaseData',
      'calculateReleaseCoordinates',
      'assignReleaseColors',
    ]);
    mockLinkService = jasmine.createSpyObj('ReleaseLinkService', ['createLinks', 'createSkipNodes', 'createSkipNodeLinks']);
    routerEventsSubject = new ReplaySubject<NavigationEnd>(1);

    const mockRouter = {
      events: routerEventsSubject.asObservable(),
      url: '/graph',
      navigate: jasmine.createSpy('navigate'),
    };

    await TestBed.configureTestingModule({
      imports: [ReleaseGraphComponent],
      providers: [
        { provide: ReleaseService, useValue: mockReleaseService },
        { provide: ReleaseNodeService, useValue: mockNodeService },
        { provide: ReleaseLinkService, useValue: mockLinkService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    mockReleaseService.getAllReleases.and.returnValue(of(mockReleases));
    mockNodeService.structureReleaseData.and.returnValue(mockStructuredGroups);
    mockNodeService.calculateReleaseCoordinates.and.returnValue(mockReleaseNodeMap);
    mockNodeService.assignReleaseColors.and.returnValue(mockNodes);
    mockLinkService.createLinks.and.returnValue(mockLinks);
    mockLinkService.createSkipNodes.and.returnValue([]);
    mockLinkService.createSkipNodeLinks.and.returnValue([]);

    fixture = TestBed.createComponent(ReleaseGraphComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Lifecycle and Initialization', () => {
    it('should call the release service on initialization', () => {
      fixture.detectChanges();

      expect(mockReleaseService.getAllReleases).toHaveBeenCalledWith();
    });

    it('should set isLoading to false after data is loaded', () => {
      expect(component.isLoading).toBe(true);
      fixture.detectChanges();

      expect(component.isLoading).toBe(false);
    });

    it('should call centerGraph after a navigation event', fakeAsync(() => {
      spyOn(component as any, 'centerGraph');
      fixture.detectChanges();
      routerEventsSubject.next(new NavigationEnd(1, '/graph', '/graph'));
      tick();

      expect((component as any).centerGraph).toHaveBeenCalledWith();
    }));

    it('should unsubscribe from router events on ngOnDestroy', () => {
      fixture.detectChanges();
      const subscription = (component as any).routerSubscription;
      spyOn(subscription, 'unsubscribe');
      component.ngOnDestroy();

      expect(subscription.unsubscribe).toHaveBeenCalledWith();
    });
  });

  describe('Data Loading and Graph Building', () => {
    it('should successfully load and process releases', () => {
      fixture.detectChanges();

      expect(component.releases).toEqual(mockReleases);
      expect(component.releaseNodes).toEqual(mockNodes);
      expect(component.allLinks).toEqual(mockLinks);
      expect(mockNodeService.structureReleaseData).toHaveBeenCalledWith(mockReleases);
    });

    it('should handle API errors gracefully', () => {
      mockReleaseService.getAllReleases.and.returnValue(throwError(() => new Error('API is down')));
      fixture.detectChanges();

      expect(component.releaseNodes).toEqual([]);
    });
  });

  describe('User Interaction and UI Logic', () => {
    it('should update translateX on wheel event', () => {
      (component as any).minTranslateX = -1000;
      (component as any).maxTranslateX = 0;
      component.onWheel(new WheelEvent('wheel', { deltaY: 100 }));

      expect(component.translateX).toBe(-100);
    });

    it('should navigate to release details on openReleaseNodeDetails', () => {
      const mockRouter = TestBed.inject(Router) as any;
      component.openReleaseNodeDetails('release-id-123');

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph', 'release-id-123']);
    });
  });

  describe('Touch Events', () => {
    let mockTouchEvent: jasmine.SpyObj<TouchEvent>;
    let mockTouch: Touch;

    beforeEach(() => {
      mockTouch = { clientX: 100, clientY: 200 } as Touch;
      mockTouchEvent = jasmine.createSpyObj<TouchEvent>('TouchEvent', ['preventDefault', 'stopPropagation']);
      Object.defineProperty(mockTouchEvent, 'touches', {
        get: () => [mockTouch],
        configurable: true,
      });

      const mockSvgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      component.svgElement = new ElementRef(mockSvgElement);
    });

    describe('onTouchStart', () => {
      it('should set isDragging to true and capture initial position on single touch', () => {
        component.onTouchStart(mockTouchEvent);

        expect(component.isDragging).toBe(true);
        expect((component as any).lastPositionX).toBe(100);
        expect((component as any).touchStartX).toBe(100);
        expect((component as any).touchStartY).toBe(200);
        expect((component as any).isTouchDragging).toBe(false);
        expect(mockTouchEvent.preventDefault).toHaveBeenCalledWith();
      });

      it('should not set isDragging when multiple touches are detected', () => {
        const multiTouch = { clientX: 150 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [mockTouch, multiTouch],
          configurable: true,
        });

        component.onTouchStart(mockTouchEvent);

        expect(component.isDragging).toBe(false);
        expect(mockTouchEvent.preventDefault).toHaveBeenCalledWith();
      });

      it('should handle touch event with zero touches', () => {
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [],
          configurable: true,
        });

        component.onTouchStart(mockTouchEvent);

        expect(component.isDragging).toBe(false);
      });
    });

    describe('onTouchMove', () => {
      beforeEach(() => {
        (component as any).minTranslateX = -1000;
        (component as any).maxTranslateX = 500;
        component.translateX = 0;
        spyOn(component as any, 'updateStickyBranchLabels');
      });

      it('should update translateX when dragging with single touch', () => {
        component.isDragging = true;
        (component as any).lastPositionX = 100;

        const newTouch = { clientX: 150 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect(component.translateX).toBe(50);
        expect((component as any).lastPositionX).toBe(150);
        expect(mockTouchEvent.preventDefault).toHaveBeenCalledWith();
        expect((component as any).updateStickyBranchLabels).toHaveBeenCalledWith();
      });

      it('should respect minimum translateX boundary', () => {
        component.isDragging = true;
        (component as any).lastPositionX = 100;
        component.translateX = -900;

        const newTouch = { clientX: 0 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect(component.translateX).toBe(-1000);
      });

      it('should respect maximum translateX boundary', () => {
        component.isDragging = true;
        (component as any).lastPositionX = 100;
        component.translateX = 400;

        const newTouch = { clientX: 250 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect(component.translateX).toBe(500);
      });

      it('should not update translateX when not dragging', () => {
        component.isDragging = false;
        (component as any).lastPositionX = 100;
        component.translateX = 0;

        const newTouch = { clientX: 150 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect(component.translateX).toBe(0);
        expect(mockTouchEvent.preventDefault).not.toHaveBeenCalled();
        expect((component as any).updateStickyBranchLabels).not.toHaveBeenCalled();
      });

      it('should not update translateX when multiple touches are detected', () => {
        component.isDragging = true;
        (component as any).lastPositionX = 100;
        component.translateX = 0;

        const touch1 = { clientX: 150 } as Touch;
        const touch2 = { clientX: 200 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [touch1, touch2],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect(component.translateX).toBe(0);
        expect(mockTouchEvent.preventDefault).not.toHaveBeenCalled();
        expect((component as any).updateStickyBranchLabels).not.toHaveBeenCalled();
      });

      it('should handle negative delta (dragging left)', () => {
        component.isDragging = true;
        (component as any).lastPositionX = 200;
        component.translateX = 100;

        const newTouch = { clientX: 150 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect(component.translateX).toBe(50);
        expect((component as any).lastPositionX).toBe(150);
      });
    });

    describe('onTouchEnd', () => {
      it('should set isDragging to false', () => {
        component.isDragging = true;

        component.onTouchEnd();

        expect(component.isDragging).toBe(false);
      });

      it('should set isDragging to false even when already false', () => {
        component.isDragging = false;

        component.onTouchEnd();

        expect(component.isDragging).toBe(false);
      });

      it('should reset isTouchDragging flag', () => {
        (component as any).isTouchDragging = true;

        component.onTouchEnd();

        expect((component as any).isTouchDragging).toBe(false);
      });
    });

    describe('Drag Detection in onTouchMove', () => {
      beforeEach(() => {
        (component as any).minTranslateX = -1000;
        (component as any).maxTranslateX = 500;
        component.translateX = 0;
        component.isDragging = true;
        (component as any).touchStartX = 100;
        (component as any).touchStartY = 200;
        (component as any).lastPositionX = 100;
        (component as any).isTouchDragging = false;
        spyOn(component as any, 'updateStickyBranchLabels');
      });

      it('should detect drag when horizontal movement exceeds 10px', () => {
        const newTouch = { clientX: 115, clientY: 200 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect((component as any).isTouchDragging).toBe(true);
      });

      it('should detect drag when vertical movement exceeds 10px', () => {
        const newTouch = { clientX: 100, clientY: 215 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect((component as any).isTouchDragging).toBe(true);
      });

      it('should not detect drag when movement is less than 10px', () => {
        const newTouch = { clientX: 105, clientY: 205 } as Touch;
        Object.defineProperty(mockTouchEvent, 'touches', {
          get: () => [newTouch],
          configurable: true,
        });

        component.onTouchMove(mockTouchEvent);

        expect((component as any).isTouchDragging).toBe(false);
      });
    });

    describe('onNodeTouchEnd', () => {
      let mockRouter: any;

      beforeEach(() => {
        mockRouter = TestBed.inject(Router) as any;
      });

      it('should open release node details when tap detected (no drag)', () => {
        (component as any).isTouchDragging = false;

        component.onNodeTouchEnd(mockTouchEvent, 'release-123');

        expect(mockTouchEvent.stopPropagation).toHaveBeenCalledWith();
        expect(mockRouter.navigate).toHaveBeenCalledWith(['/graph', 'release-123']);
      });

      it('should not open release node details when drag detected', () => {
        (component as any).isTouchDragging = true;

        component.onNodeTouchEnd(mockTouchEvent, 'release-123');

        expect(mockTouchEvent.stopPropagation).toHaveBeenCalledWith();
        expect(mockRouter.navigate).not.toHaveBeenCalled();
      });
    });

    describe('onSkipNodeTouchEnd', () => {
      beforeEach(() => {
        component.skipNodes = [
          { id: 'skip-1', label: 'Skip 1', x: 100, y: 200, skippedCount: 5, skippedVersions: [] },
        ];
      });

      it('should open skip node modal when tap detected (no drag)', () => {
        (component as any).isTouchDragging = false;

        component.onSkipNodeTouchEnd(mockTouchEvent, 'skip-1');

        expect(mockTouchEvent.stopPropagation).toHaveBeenCalledWith();
        expect(component.dataForSkipModal).toEqual({
          id: 'skip-1',
          label: 'Skip 1',
          x: 100,
          y: 200,
          skippedCount: 5,
          skippedVersions: [],
        });
      });

      it('should not open skip node modal when drag detected', () => {
        (component as any).isTouchDragging = true;

        component.onSkipNodeTouchEnd(mockTouchEvent, 'skip-1');

        expect(mockTouchEvent.stopPropagation).toHaveBeenCalledWith();
        expect(component.dataForSkipModal).toBeNull();
      });

      it('should not open modal for non-existent skip node', () => {
        (component as any).isTouchDragging = false;

        component.onSkipNodeTouchEnd(mockTouchEvent, 'non-existent');

        expect(mockTouchEvent.stopPropagation).toHaveBeenCalledWith();
        expect(component.dataForSkipModal).toBeNull();
      });
    });
  });

  describe('ViewBox Calculation', () => {
    it('should set valid, finite numbers for scale and transform properties', () => {
      const svgWidth = 1200;
      const svgHeight = 800;
      const mockSvgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      spyOnProperty(mockSvgElement, 'clientWidth', 'get').and.returnValue(svgWidth);
      spyOnProperty(mockSvgElement, 'clientHeight', 'get').and.returnValue(svgHeight);
      component.svgElement = new ElementRef(mockSvgElement);
      component.releaseNodes = [{ position: { x: 0, y: 0 } }, { position: { x: 2000, y: 500 } }] as any;

      (component as any).centerGraph();

      expect(Number.isFinite(component.scale)).toBe(true);
      expect(Number.isFinite(component.translateX)).toBe(true);
      expect(Number.isFinite(component.translateY)).toBe(true);
      expect(component.scale).toBeGreaterThan(0);
      expect(component.viewBox).toBe(`0 0 ${svgWidth} ${svgHeight}`);
    });
  });
});
