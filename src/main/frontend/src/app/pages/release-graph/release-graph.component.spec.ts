import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReleaseGraphComponent } from './release-graph.component';
import { ReleaseService, Release } from '../../services/release.service';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLinkService } from './release-link.service';
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
    mockToastService = jasmine.createSpyObj('ToastrService', ['error']);
    routerEventsSubject = new ReplaySubject<NavigationEnd>(1);

    const mockRouter = {
      events: routerEventsSubject.asObservable(),
      url: '/graph',
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

    it('should emit the correct release on openReleaseDetails', () => {
      spyOn(component._selectedRelease, 'next');
      component.releases = mockReleases;
      component.openReleaseDetails(mockNodes[0]);

      expect(component._selectedRelease.next).toHaveBeenCalledWith(mockReleases[0]);
    });

    it('should emit null on closeReleaseDetails', () => {
      spyOn(component._selectedRelease, 'next');
      component.closeReleaseDetails();

      expect(component._selectedRelease.next).toHaveBeenCalledWith(null);
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
