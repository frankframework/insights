import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseGraphComponent } from './release-graph.component';
import { ReleaseService, Release } from '../../services/release.service';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLinkService, SkipNode } from './release-link.service';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { of, ReplaySubject, throwError, BehaviorSubject } from 'rxjs';
import { ElementRef } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('ReleaseGraphComponent', () => {
  let component: ReleaseGraphComponent;
  let fixture: ComponentFixture<ReleaseGraphComponent>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockNodeService: jasmine.SpyObj<ReleaseNodeService>;
  let mockLinkService: jasmine.SpyObj<ReleaseLinkService>;
  let routerEventsSubject: ReplaySubject<NavigationEnd>;
  let queryParametersSubject: BehaviorSubject<any>;

  const mockReleaseData: Record<string, Release[]> = {
    master: [{ id: '1', name: 'v1.0.0', tagName: 'v1', publishedAt: new Date(), branch: { id: 'b1', name: 'master' } }],
  };
  const mockReleases: Release[] = Object.values(mockReleaseData).flat();
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
  const mockTimelineScale = {
    startDate: new Date('2024-01-01'),
    endDate: new Date('2024-12-31'),
    pixelsPerDay: 2,
    totalDays: 365,
    quarters: [
      { label: 'Q1 2024', date: new Date('2024-01-01'), x: 0, labelX: 100, year: 2024, quarter: 1 },
      { label: 'Q2 2024', date: new Date('2024-04-01'), x: 200, labelX: 300, year: 2024, quarter: 2 },
    ],
    latestReleaseDate: new Date('2024-12-31'),
  };

  beforeEach(async () => {
    mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getAllReleases']);
    mockNodeService = jasmine.createSpyObj('ReleaseNodeService', [
      'structureReleaseData',
      'calculateReleaseCoordinates',
      'assignReleaseColors',
      'applyMinimumSpacing',
      'getVersionInfo',
    ]);
    mockLinkService = jasmine.createSpyObj('ReleaseLinkService', ['createLinks', 'createSkipNodes', 'createSkipNodeLinks']);
    routerEventsSubject = new ReplaySubject<NavigationEnd>(1);
    queryParametersSubject = new BehaviorSubject<any>({});

    const mockRouter = {
      events: routerEventsSubject.asObservable(),
      url: '/graph',
      navigate: jasmine.createSpy('navigate'),
    };

    const mockActivatedRoute = {
      queryParams: queryParametersSubject.asObservable(),
    };

    await TestBed.configureTestingModule({
      imports: [ReleaseGraphComponent],
      providers: [
        { provide: ReleaseService, useValue: mockReleaseService },
        { provide: ReleaseNodeService, useValue: mockNodeService },
        { provide: ReleaseLinkService, useValue: mockLinkService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    mockReleaseService.getAllReleases.and.returnValue(of(mockReleases));
    mockNodeService.structureReleaseData.and.returnValue(mockStructuredGroups);
    mockNodeService.calculateReleaseCoordinates.and.callFake(() => {
      mockNodeService.timelineScale = mockTimelineScale;
      return mockReleaseNodeMap;
    });
    mockNodeService.assignReleaseColors.and.returnValue();
    mockNodeService.timelineScale = mockTimelineScale;
    mockNodeService.applyMinimumSpacing.and.returnValue(mockNodes);
    mockNodeService.getVersionInfo.and.returnValue({ major: 1, minor: 0, patch: 0, type: 'major' });
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

    it('should subscribe to router events on initialization', () => {
      fixture.detectChanges();

      expect((component as any).routerSubscription).toBeDefined();
      expect((component as any).routerSubscription.closed).toBe(false);
    });

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
    });

    describe('onTouchEnd', () => {
      it('should set isDragging to false', () => {
        component.isDragging = true;

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
    });
  });

  describe('Timeline and Quarter Markers', () => {
    it('should load quarter markers from the node service timeline scale', () => {
      fixture.detectChanges();

      expect(component.quarterMarkers).toBeDefined();
      expect(component.quarterMarkers.length).toBe(2);
      expect(component.quarterMarkers[0].label).toBe('Q1 2024');
    });

    it('should handle missing timeline scale gracefully', () => {
      mockNodeService.timelineScale = null;

      mockNodeService.calculateReleaseCoordinates.and.callFake(() => {
        return mockReleaseNodeMap;
      });

      fixture.detectChanges();

      expect(component.quarterMarkers).toEqual([]);
    });
  });

  describe('Nightly Toggle Functionality', () => {
    it('should toggle showNightlies state', () => {
      expect(component.showNightlies).toBe(false);

      component.toggleNightlies();

      expect(component.showNightlies).toBe(true);

      component.toggleNightlies();

      expect(component.showNightlies).toBe(false);
    });

    it('should return all nodes when showNightlies is true', () => {
      const nightlyNode: ReleaseNode = {
        id: 'nightly-1',
        label: 'v1.0.0-nightly',
        position: { x: 100, y: 100 },
        branch: 'release',
        color: 'darkblue',
        publishedAt: new Date(),
      };
      const regularNode: ReleaseNode = {
        id: 'regular-1',
        label: 'v1.0.0',
        position: { x: 200, y: 0 },
        branch: 'master',
        color: 'green',
        publishedAt: new Date(),
      };
      component.releaseNodes = [regularNode, nightlyNode];
      component.showNightlies = true;

      expect(component.visibleReleaseNodes).toEqual([regularNode, nightlyNode]);
    });

    it('should filter out nightly nodes when showNightlies is false', () => {
      const nightlyNode: ReleaseNode = {
        id: 'nightly-1',
        label: 'v1.0.0-nightly',
        position: { x: 100, y: 100 },
        branch: 'release',
        color: 'darkblue',
        publishedAt: new Date(),
      };
      const regularNode: ReleaseNode = {
        id: 'regular-1',
        label: 'v1.0.0',
        position: { x: 200, y: 0 },
        branch: 'master',
        color: 'green',
        publishedAt: new Date(),
      };
      component.releaseNodes = [regularNode, nightlyNode];
      component.showNightlies = false;

      const visible = component.visibleReleaseNodes;

      expect(visible.length).toBe(1);
      expect(visible[0]).toBe(regularNode);
    });

    describe('isNightlyNode detection', () => {
      it('should identify nightly nodes with nightly keyword', () => {
        const nightlyNode: ReleaseNode = {
          id: 'nightly-1',
          label: 'v1.0.0-NIGHTLY',
          position: { x: 100, y: 100 },
          branch: 'release',
          color: 'darkblue',
          publishedAt: new Date(),
        };
        component.releaseNodes = [nightlyNode];
        component.showNightlies = false;

        const visible = component.visibleReleaseNodes;

        expect(visible.length).toBe(0);
      });

      it('should identify date-pattern nodes as nightly', () => {
        const dateNode: ReleaseNode = {
          id: 'date-1',
          label: 'v1.0.0-20241201.120000',
          position: { x: 100, y: 100 },
          branch: 'release',
          color: 'darkblue',
          publishedAt: new Date(),
        };
        component.releaseNodes = [dateNode];
        component.showNightlies = false;

        const visible = component.visibleReleaseNodes;

        expect(visible.length).toBe(0);
      });

      it('should not consider mini nodes as nightly', () => {
        const miniNode: ReleaseNode = {
          id: 'mini-1',
          label: 'v1.0.0-nightly',
          position: { x: 100, y: 100 },
          branch: 'release',
          color: 'white',
          publishedAt: new Date(),
          isMiniNode: true,
        };
        component.releaseNodes = [miniNode];
        component.showNightlies = false;

        const visible = component.visibleReleaseNodes;

        expect(visible.length).toBe(1);
        expect(visible[0]).toBe(miniNode);
      });

      it('should not consider nodes at y=0 (master branch) as nightly', () => {
        const masterNode: ReleaseNode = {
          id: 'master-1',
          label: 'v1.0.0-nightly',
          position: { x: 100, y: 0 },
          branch: 'master',
          color: 'green',
          publishedAt: new Date(),
        };
        component.releaseNodes = [masterNode];
        component.showNightlies = false;

        const visible = component.visibleReleaseNodes;

        expect(visible.length).toBe(1);
        expect(visible[0]).toBe(masterNode);
      });
    });

    describe('visibleLinks filtering', () => {
      it('should return all links when showNightlies is true', () => {
        const link1 = { id: '1-2', source: '1', target: '2' };
        const link2 = { id: '2-3', source: '2', target: '3' };
        component.allLinks = [link1, link2];
        component.showNightlies = true;

        expect(component.visibleLinks).toEqual([link1, link2]);
      });

      it('should filter out links where source is nightly', () => {
        const nightlyNode: ReleaseNode = {
          id: 'nightly-1',
          label: 'v1.0.0-nightly',
          position: { x: 100, y: 100 },
          branch: 'release',
          color: 'darkblue',
          publishedAt: new Date(),
        };
        const regularNode: ReleaseNode = {
          id: 'regular-1',
          label: 'v1.0.0',
          position: { x: 200, y: 0 },
          branch: 'master',
          color: 'green',
          publishedAt: new Date(),
        };
        component.releaseNodes = [nightlyNode, regularNode];
        component.allLinks = [{ id: 'nightly-1-regular-1', source: 'nightly-1', target: 'regular-1' }];
        component.showNightlies = false;

        const visible = component.visibleLinks;

        expect(visible.length).toBe(0);
      });

      it('should filter out links where target is nightly', () => {
        const nightlyNode: ReleaseNode = {
          id: 'nightly-1',
          label: 'v1.0.0-nightly',
          position: { x: 100, y: 100 },
          branch: 'release',
          color: 'darkblue',
          publishedAt: new Date(),
        };
        const regularNode: ReleaseNode = {
          id: 'regular-1',
          label: 'v1.0.0',
          position: { x: 200, y: 0 },
          branch: 'master',
          color: 'green',
          publishedAt: new Date(),
        };
        component.releaseNodes = [regularNode, nightlyNode];
        component.allLinks = [{ id: 'regular-1-nightly-1', source: 'regular-1', target: 'nightly-1' }];
        component.showNightlies = false;

        const visible = component.visibleLinks;

        expect(visible.length).toBe(0);
      });

      it('should keep links between non-nightly nodes', () => {
        const regularNode1: ReleaseNode = {
          id: 'regular-1',
          label: 'v1.0.0',
          position: { x: 100, y: 0 },
          branch: 'master',
          color: 'green',
          publishedAt: new Date(),
        };
        const regularNode2: ReleaseNode = {
          id: 'regular-2',
          label: 'v1.0.1',
          position: { x: 200, y: 0 },
          branch: 'master',
          color: 'green',
          publishedAt: new Date(),
        };
        component.releaseNodes = [regularNode1, regularNode2];
        component.allLinks = [{ id: 'regular-1-regular-2', source: 'regular-1', target: 'regular-2' }];
        component.showNightlies = false;

        const visible = component.visibleLinks;

        expect(visible.length).toBe(1);
        expect(visible[0].id).toBe('regular-1-regular-2');
      });
    });
  });

  describe('(private) findNodeById', () => {
    // eslint-disable-next-line no-unused-vars
    let findNodeById: (_id: string) => ReleaseNode | undefined;

    beforeEach(() => {
      findNodeById = (component as any).findNodeById.bind(component);
    });

    it('should find a regular release node', () => {
      component.releaseNodes = [
        { id: 'node-1', position: { x: 100, y: 0 } } as ReleaseNode,
        { id: 'node-2', position: { x: 200, y: 0 } } as ReleaseNode,
      ];

      expect(findNodeById('node-2')).toBe(component.releaseNodes[1]);
    });

    it('should find a skip node', () => {
      component.skipNodes = [
        { id: 'skip-1', x: 150, y: 0, label: 'skip' } as SkipNode,
      ];
      const result = findNodeById('skip-1');

      expect(result).toBeDefined();
      expect(result?.id).toBe('skip-1');
      expect(result?.position).toEqual({ x: 150, y: 0 });
    });

    it('should return undefined for a non-existent ID', () => {
      component.releaseNodes = [{ id: 'node-1' } as ReleaseNode];
      component.skipNodes = [{ id: 'skip-1' } as SkipNode];

      expect(findNodeById('non-existent')).toBeUndefined();
    });

    it('should create a start-node relative to the first node (no initial skip)', () => {
      component.releaseNodes = [
        { id: 'node-1', position: { x: 400, y: 0 } } as ReleaseNode,
      ];
      component.skipNodes = [];

      const result = findNodeById('start-node-node-1');

      expect(result).toBeDefined();
      expect(result?.id).toBe('start-node-node-1');
      expect(result?.position).toEqual({ x: 100, y: 0 });
    });

    it('should create a start-node relative to the initial skip node', () => {
      component.releaseNodes = [
        { id: 'node-1', position: { x: 800, y: 0 } } as ReleaseNode,
      ];
      component.skipNodes = [
        { id: 'skip-initial-node-1', x: 400, y: 0 } as SkipNode,
      ];

      const result = findNodeById('start-node-node-1');

      expect(result).toBeDefined();
      expect(result?.id).toBe('start-node-node-1');
      expect(result?.position).toEqual({ x: 100, y: 0 });
    });
  });

  describe('Extended Support Functionality', () => {
    describe('Query Parameter Detection', () => {
      it('should set showExtendedSupport to false by default', () => {
        fixture.detectChanges();

        expect(component.showExtendedSupport).toBe(false);
      });

      it('should set showExtendedSupport to true when extended param is present', () => {
        queryParametersSubject.next({ extended: '' });
        fixture.detectChanges();

        expect(component.showExtendedSupport).toBe(true);
      });

      it('should set showExtendedSupport to false when extended param is removed', () => {
        queryParametersSubject.next({ extended: '' });
        fixture.detectChanges();

        expect(component.showExtendedSupport).toBe(true);

        queryParametersSubject.next({});

        expect(component.showExtendedSupport).toBe(false);
      });

      it('should rebuild graph when extended mode changes', () => {
        fixture.detectChanges();
        component.releases = mockReleases;
        spyOn<any>(component, 'buildReleaseGraph');

        queryParametersSubject.next({ extended: '' });

        expect((component as any).buildReleaseGraph).toHaveBeenCalledWith(mockStructuredGroups);
      });
    });

    describe('calculateSupportEndDate', () => {
      let calculateSupportEndDate: any;

      beforeEach(() => {
        calculateSupportEndDate = (component as any).calculateSupportEndDate.bind(component);
      });

      it('should calculate standard support end date for major version', () => {
        component.showExtendedSupport = false;
        const startDate = new Date('2024-01-01');
        const endDate = calculateSupportEndDate(startDate, 4, true);

        expect(endDate.getFullYear()).toBe(2025);
        expect(endDate.getMonth()).toBe(0);
      });

      it('should calculate extended support end date for major version', () => {
        component.showExtendedSupport = true;
        const startDate = new Date('2024-01-01');
        const endDate = calculateSupportEndDate(startDate, 4, true);

        expect(endDate.getFullYear()).toBe(2025);
        expect(endDate.getMonth()).toBe(6);
      });

      it('should calculate standard support end date for minor version', () => {
        component.showExtendedSupport = false;
        const startDate = new Date('2024-01-01');
        const endDate = calculateSupportEndDate(startDate, 2, false);

        expect(endDate.getFullYear()).toBe(2024);
        expect(endDate.getMonth()).toBe(6);
      });

      it('should calculate extended support end date for minor version', () => {
        component.showExtendedSupport = true;
        const startDate = new Date('2024-01-01');
        const endDate = calculateSupportEndDate(startDate, 2, false);

        expect(endDate.getFullYear()).toBe(2024);
        expect(endDate.getMonth()).toBe(9);
      });
    });

    describe('calculatePhaseBoundaries', () => {
      let calculatePhaseBoundaries: any;
      const mockScale = { startDate: new Date('2024-01-01'), pixelsPerDay: 2 };

      beforeEach(() => {
        calculatePhaseBoundaries = (component as any).calculatePhaseBoundaries.bind(component);
      });

      it('should calculate 2-phase boundaries in standard mode', () => {
        component.showExtendedSupport = false;
        const result = calculatePhaseBoundaries(
          new Date('2024-01-01'),
          0,
          1000,
          6,
          mockScale
        );

        expect(result.greenPhaseEndX).toBe(500);
        expect(result.orangePhaseStartX).toBe(500);
      });

      it('should calculate 3-phase boundaries in extended mode for major version', () => {
        component.showExtendedSupport = true;
        const result = calculatePhaseBoundaries(
          new Date('2024-01-01'),
          0,
          1000,
          6,
          mockScale
        );

        expect(result.greenPhaseEndX).toBeGreaterThan(0);
        expect(result.orangePhaseStartX).toBeGreaterThan(result.greenPhaseEndX);
        expect(result.orangePhaseStartX).toBeLessThan(1000);
      });
    });

    describe('createLifecyclePhases', () => {
      let createLifecyclePhases: any;

      beforeEach(() => {
        createLifecyclePhases = (component as any).createLifecyclePhases.bind(component);
      });

      it('should create 2 phases in standard mode', () => {
        component.showExtendedSupport = false;
        const phases = createLifecyclePhases(0, 500, 500, 1000, false);

        expect(phases.length).toBe(2);
        expect(phases[0].color).toContain('144, 238, 144');
        expect(phases[1].color).toContain('251, 146, 60');
      });

      it('should create 3 phases in extended mode', () => {
        component.showExtendedSupport = true;
        const phases = createLifecyclePhases(0, 333, 666, 1000, false);

        expect(phases.length).toBe(3);
        expect(phases[0].color).toContain('144, 238, 144');
        expect(phases[1].color).toContain('59, 130, 246');
        expect(phases[2].color).toContain('251, 146, 60');
      });

      it('should use outdated colors when isOutdated is true', () => {
        component.showExtendedSupport = false;
        const phases = createLifecyclePhases(0, 500, 500, 1000, true);

        expect(phases[0].color).toContain('210, 210, 210');
        expect(phases[1].color).toContain('210, 210, 210');
      });

      it('should create phases with correct boundaries', () => {
        component.showExtendedSupport = true;
        const phases = createLifecyclePhases(0, 300, 600, 900, false);

        expect(phases[0].startX).toBe(0);
        expect(phases[0].endX).toBe(300);
        expect(phases[1].startX).toBe(300);
        expect(phases[1].endX).toBe(600);
        expect(phases[2].startX).toBe(600);
        expect(phases[2].endX).toBe(900);
      });
    });

    describe('findMajorMinorRelease', () => {
      let findMajorMinorRelease: any;

      beforeEach(() => {
        findMajorMinorRelease = (component as any).findMajorMinorRelease.bind(component);
      });

      it('should find a major.minor.0 release', () => {
        component.releases = [
          { id: '1', name: 'v7.0.0', publishedAt: new Date() } as Release,
          { id: '2', name: 'v7.0.1', publishedAt: new Date() } as Release,
        ];

        const result = findMajorMinorRelease('7.0');

        expect(result).toBeDefined();
        expect(result?.name).toBe('v7.0.0');
      });

      it('should find release without v prefix', () => {
        component.releases = [
          { id: '1', name: '7.0.0', publishedAt: new Date() } as Release,
        ];

        const result = findMajorMinorRelease('7.0');

        expect(result).toBeDefined();
        expect(result?.name).toBe('7.0.0');
      });

      it('should not find nightly releases', () => {
        component.releases = [
          { id: '1', name: 'v7.0.0-nightly', publishedAt: new Date() } as Release,
        ];

        const result = findMajorMinorRelease('7.0');

        expect(result).toBeUndefined();
      });

      it('should return undefined when no matching release exists', () => {
        component.releases = [
          { id: '1', name: 'v8.0.0', publishedAt: new Date() } as Release,
        ];

        const result = findMajorMinorRelease('7.0');

        expect(result).toBeUndefined();
      });
    });
  });
});
