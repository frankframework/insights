import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReleaseGraphComponent } from './release-graph.component';
import { ReleaseService, Release } from '../../services/release.service';
import { ReleaseNodeService } from './release-node.service';
import { ReleaseLinkService } from './release-link.service';
import { Router, NavigationEnd } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { of, ReplaySubject, throwError } from 'rxjs';
import { ElementRef } from '@angular/core';

// --- Mocks for all dependencies ---
const mockReleaseService = jasmine.createSpyObj('ReleaseService', ['getAllReleases']);
const mockNodeService = jasmine.createSpyObj('ReleaseNodeService', ['structureReleaseData', 'calculateReleaseCoordinates', 'assignReleaseColors']);
const mockLinkService = jasmine.createSpyObj('ReleaseLinkService', ['createLinks']);
const mockToastService = jasmine.createSpyObj('ToastrService', ['error']);
const routerEventsSubject = new ReplaySubject<NavigationEnd>(1);

const mockRouter = {
  events: routerEventsSubject.asObservable(),
  url: '/graph',
};

// --- Mock Data ---
const mockReleases: Release[] = [{ id: '1', name: 'v1.0.0', tagName: 'v1', publishedAt: new Date(), branch: { id: 'b1', name: 'master' } }];
const mockNodes = [{ id: '1', label: 'v1.0.0', position: { x: 100, y: 50 }, branch: 'master', color: 'green', publishedAt: new Date() }];
const mockLinks = [{ id: '1-2', source: '1', target: '2' }];
const mockStructuredGroups = [new Map([['master', mockNodes]])];
const mockReleaseNodeMap = new Map([['master', mockNodes]]);

describe('ReleaseGraphComponent', () => {
  let component: ReleaseGraphComponent;
  let fixture: ComponentFixture<ReleaseGraphComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseGraphComponent], // Standalone component
      providers: [
        { provide: ReleaseService, useValue: mockReleaseService },
        { provide: ReleaseNodeService, useValue: mockNodeService },
        { provide: ReleaseLinkService, useValue: mockLinkService },
        { provide: ToastrService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseGraphComponent);
    component = fixture.componentInstance;

    // Reset spies before each test
    mockReleaseService.getAllReleases.and.returnValue(of(mockReleases));
    mockNodeService.structureReleaseData.and.returnValue(mockStructuredGroups);
    mockNodeService.calculateReleaseCoordinates.and.returnValue(mockReleaseNodeMap);
    mockNodeService.assignReleaseColors.and.returnValue(mockNodes);
    mockLinkService.createLinks.and.returnValue(mockLinks);
    mockToastService.error.calls.reset();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Lifecycle and Initialization', () => {
    it('should call getAllReleases on ngOnInit', () => {
      spyOn(component as any, 'getAllReleases').and.callThrough();
      fixture.detectChanges(); // Triggers ngOnInit
      expect((component as any).getAllReleases).toHaveBeenCalled();
    });

    it('should set isLoading to false after data is loaded', () => {
      expect(component.isLoading).toBe(true);
      fixture.detectChanges();
      expect(component.isLoading).toBe(false);
    });

    it('should call centerGraph after a navigation event to the graph URL', fakeAsync(() => {
      spyOn(component as any, 'centerGraph').and.callThrough();
      fixture.detectChanges();

      routerEventsSubject.next(new NavigationEnd(1, '/graph', '/graph'));
      tick(); // Process the setTimeout inside the subscription

      expect((component as any).centerGraph).toHaveBeenCalled();
    }));

    it('should unsubscribe from router events on ngOnDestroy', () => {
      fixture.detectChanges(); // ngOnInit subscribes
      const subscription = (component as any).routerSubscription;
      spyOn(subscription, 'unsubscribe');

      component.ngOnDestroy();

      expect(subscription.unsubscribe).toHaveBeenCalled();
    });
  });

  describe('Data Loading and Graph Building', () => {
    it('should successfully load and process releases into nodes and links', () => {
      fixture.detectChanges();

      expect(component.releases).toEqual(mockReleases);
      expect(component.releaseNodes).toEqual(mockNodes);
      expect(component.releaseLinks).toEqual(mockLinks);
      expect(mockNodeService.structureReleaseData).toHaveBeenCalledWith(mockReleases);
      expect(mockNodeService.calculateReleaseCoordinates).toHaveBeenCalledWith(mockStructuredGroups);
      expect(mockLinkService.createLinks).toHaveBeenCalledWith(mockStructuredGroups);
    });

    it('should handle API errors gracefully and show a toast message', () => {
      const errorResponse = new Error('API is down');
      mockReleaseService.getAllReleases.and.returnValue(throwError(() => errorResponse));

      fixture.detectChanges();

      expect(component.isLoading).toBe(false);
      expect(component.releaseNodes).toEqual([]);
      expect(component.releaseLinks).toEqual([]);
      expect(mockToastService.error).toHaveBeenCalledWith('Failed to load releases. Please try again later.');
    });

    it('should show a toast message if the API returns an empty array of releases', () => {
      mockReleaseService.getAllReleases.and.returnValue(of([]));

      fixture.detectChanges();

      expect(mockToastService.error).toHaveBeenCalledWith('No releases found.');
    });
  });

  describe('User Interaction and UI Logic', () => {
    it('should update translateX on wheel event for horizontal panning', () => {
      component.translateX = 0;
      component.scale = 1;
      (component as any).minTranslateX = -1000;
      (component as any).maxTranslateX = 0;

      const wheelEvent = new WheelEvent('wheel', { deltaY: 100 });
      component.onWheel(wheelEvent);

      expect(component.translateX).toBe(-100);
    });

    it('should emit the correct release when openReleaseDetails is called', () => {
      spyOn(component._selectedRelease, 'next');
      component.releases = mockReleases;
      const nodeToSelect = mockNodes[0];

      component.openReleaseDetails(nodeToSelect);

      expect(component._selectedRelease.next).toHaveBeenCalledWith(mockReleases[0]);
    });

    it('should emit null when closeReleaseDetails is called', () => {
      spyOn(component._selectedRelease, 'next');
      component.closeReleaseDetails();
      expect(component._selectedRelease.next).toHaveBeenCalledWith(null);
    });

    it('should generate a straight SVG path for nodes on the same y-level', () => {
      const source = { id: 's1', position: { x: 100, y: 50 } } as any;
      const target = { id: 't1', position: { x: 300, y: 50 } } as any;
      component.releaseNodes = [source, target];
      const link = { id: 's1-t1', source: 's1', target: 't1' };

      const path = component.getCustomPath(link);
      expect(path).toContain('M 125,50 L 275,50');
    });

    it('should generate an arc SVG path for nodes on different y-levels', () => {
      const source = { id: 's1', position: { x: 100, y: 50 } } as any;
      const target = { id: 't1', position: { x: 300, y: 150 } } as any;
      component.releaseNodes = [source, target];
      const link = { id: 's1-t1', source: 's1', target: 't1' };

      const path = component.getCustomPath(link);
      expect(path).toContain(' A ');
    });
  });

  describe('ViewBox Calculation', () => {
    it('should set valid, finite numbers for scale and transform properties', () => {
      const mockSvgElement = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      const svgWidth = 1200;
      const svgHeight = 800;
      spyOnProperty(mockSvgElement, 'clientWidth').and.returnValue(svgWidth);
      spyOnProperty(mockSvgElement, 'clientHeight').and.returnValue(svgHeight);
      component.svgElement = new ElementRef(mockSvgElement);

      component.releaseNodes = [
        { id: 'n1', position: { x: 0, y: 0 } } as any,
        { id: 'n2', position: { x: 2000, y: 500 } } as any,
      ];

      (component as any).centerGraph();

      expect(isFinite(component.scale)).toBe(true);
      expect(isFinite(component.translateX)).toBe(true);
      expect(isFinite(component.translateY)).toBe(true);

      expect(component.scale).toBeGreaterThan(0);

      expect(component.viewBox).toBe(`0 0 ${svgWidth} ${svgHeight}`);
    });
  });
});
