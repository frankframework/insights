import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReleaseGraphComponent } from './release-graph.component';
import { ReleaseService, Release } from '../../services/release.service';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLinkService, SkipNode } from './release-link.service';
import { Router, NavigationEnd } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of, ReplaySubject, throwError } from 'rxjs';
import { ElementRef } from '@angular/core';

describe('ReleaseGraphComponent', () => {
  let component: ReleaseGraphComponent;
  let fixture: ComponentFixture<ReleaseGraphComponent>;
  let mockReleaseService: jasmine.SpyObj<ReleaseService>;
  let mockNodeService: jasmine.SpyObj<ReleaseNodeService>;
  let mockLinkService: jasmine.SpyObj<ReleaseLinkService>;
  let mockToastService: jasmine.SpyObj<ToastrService>;
  let routerEventsSubject: ReplaySubject<NavigationEnd>;

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
      'createClusters',
      'expandCluster',
      'getVersionInfo',
    ]);
    mockLinkService = jasmine.createSpyObj('ReleaseLinkService', ['createLinks', 'createSkipNodes', 'createSkipNodeLinks']);
    mockToastService = jasmine.createSpyObj('ToastrService', ['error']);
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
        { provide: ToastrService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter },
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
    mockNodeService.createClusters.and.returnValue(mockNodes);
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

    it('should handle API errors gracefully and show a toast message', () => {
      mockReleaseService.getAllReleases.and.returnValue(throwError(() => new Error('API is down')));
      fixture.detectChanges();

      expect(component.releaseNodes).toEqual([]);
      expect(mockToastService.error).toHaveBeenCalledWith('Failed to load releases. Please try again later.');
    });

    it('should show a toast message if the API returns no releases', () => {
      mockReleaseService.getAllReleases.and.returnValue(of([]));
      fixture.detectChanges();

      expect(mockToastService.error).toHaveBeenCalledWith('No releases found.');
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

  describe('Cluster Functionality', () => {
    it('should toggle cluster expansion', () => {
      const clusterNode: ReleaseNode = {
        id: 'cluster-1',
        label: '3',
        position: { x: 100, y: 0 },
        branch: 'master',
        color: '#dee2e6',
        publishedAt: new Date(),
        isCluster: true,
        isExpanded: false,
        clusteredNodes: [
          { id: '1', label: 'v1.0', position: { x: 90, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
          { id: '2', label: 'v1.1', position: { x: 100, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
          { id: '3', label: 'v1.2', position: { x: 110, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
        ],
      };
      component.releaseNodes = [clusterNode];
      const expandedNodes = clusterNode.clusteredNodes!;
      mockNodeService.expandCluster.and.returnValue(expandedNodes);

      component.toggleCluster(clusterNode);

      expect(clusterNode.isExpanded).toBe(true);
      expect(component.releaseNodes.length).toBe(3);
      expect(mockNodeService.expandCluster).toHaveBeenCalledWith(clusterNode);
    });

    it('should collapse an expanded cluster', () => {
      const clusterNode: ReleaseNode = {
        id: 'cluster-1',
        label: '3',
        position: { x: 100, y: 0 },
        branch: 'master',
        color: '#dee2e6',
        publishedAt: new Date(),
        isCluster: true,
        isExpanded: true,
        clusteredNodes: [
          { id: '1', label: 'v1.0', position: { x: 90, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
          { id: '2', label: 'v1.1', position: { x: 100, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
          { id: '3', label: 'v1.2', position: { x: 110, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
        ],
      };
      component.releaseNodes = clusterNode.clusteredNodes!;
      component.expandedClusters.set(clusterNode.id, clusterNode);
      component.collapseCluster(clusterNode.id);

      expect(clusterNode.isExpanded).toBe(false);
      expect(component.releaseNodes.length).toBe(1);
      expect(component.releaseNodes[0]).toBe(clusterNode);
      expect(component.expandedClusters.has(clusterNode.id)).toBe(false);
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

    it('should find a node that is inside a cluster', () => {
      const clusteredNode = { id: 'node-in-cluster' } as ReleaseNode;
      const cluster = {
        id: 'cluster-1',
        isCluster: true,
        clusteredNodes: [ clusteredNode ]
      } as ReleaseNode;

      component.releaseNodes = [ cluster ];

      const result = findNodeById('node-in-cluster');

      expect(result).toBe(cluster);
    });
  });
});
