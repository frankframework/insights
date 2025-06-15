import { Component, ElementRef, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { Release, ReleaseService } from '../../services/release.service';
import { BehaviorSubject, catchError, map, of, tap } from 'rxjs';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLink, ReleaseLinkService } from './release-link.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { ReleaseOffCanvasComponent } from './release-off-canvas/release-off-canvas.component';
import { AsyncPipe } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ReleaseCatalogusComponent } from '../../components/release-catalogus/release-catalogus.component';

@Component({
  selector: 'app-release-graph',
  standalone: true,
  templateUrl: './release-graph.component.html',
  styleUrls: ['./release-graph.component.scss'],
  imports: [LoaderComponent, ReleaseOffCanvasComponent, AsyncPipe, ReleaseCatalogusComponent],
})
export class ReleaseGraphComponent implements OnInit, OnDestroy {

  @ViewChild('svgElement') svgElement!: ElementRef<SVGSVGElement>;

  public _selectedRelease = new BehaviorSubject<Release | null>(null);
  public selectedRelease$ = this._selectedRelease.asObservable();

  public releaseNodes: ReleaseNode[] = [];
  public releaseLinks: ReleaseLink[] = [];
  public isLoading = true;
  public releases: Release[] = [];
  public scale = 1;
  public translateX = 0;
  public translateY = 0;
  public viewBox = '0 0 0 0';

  private minTranslateX = 0;
  private maxTranslateX = 0;
  private routerSubscription!: Subscription;

  constructor(
    private releaseService: ReleaseService,
    private nodeService: ReleaseNodeService,
    private linkService: ReleaseLinkService,
    private router: Router,
    private toastService: ToastrService,
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.getAllReleases();

    this.routerSubscription = this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd && this.router.url.includes('/graph')) {
        setTimeout(() => this.centerGraph(), 0);
      }
    });
  }

  ngOnDestroy(): void {
    this.routerSubscription?.unsubscribe();
  }

  public onWheel(event: WheelEvent): void {
    event.preventDefault();
    const delta = event.deltaY / this.scale;
    const newTranslateX = this.translateX - delta;
    this.translateX = Math.min(this.maxTranslateX, Math.max(this.minTranslateX, newTranslateX));
  }

  public getCustomPath(link: ReleaseLink): string {
    const source = this.releaseNodes.find((n) => n.id === link.source);
    const target = this.releaseNodes.find((n) => n.id === link.target);
    if (!source || !target) return '';

    const releaseNodeRadiusWithMargin = 25;
    const [x1, y1] = [source.position.x, source.position.y];
    const [x2, y2] = [target.position.x, target.position.y];

    if (y1 === y2) {
      return `M ${x1 + releaseNodeRadiusWithMargin},${y1} L ${x2 - releaseNodeRadiusWithMargin},${y2}`;
    }

    const verticalDirection = y2 > y1 ? 1 : -1;
    const cornerY = y2 - verticalDirection * releaseNodeRadiusWithMargin;

    const horizontalSweep = x2 > x1 ? 0 : 1;

    return [
      `M ${x1},${y1 + releaseNodeRadiusWithMargin}`,
      `L ${x1},${cornerY}`,
      `A ${releaseNodeRadiusWithMargin},${releaseNodeRadiusWithMargin} 0 0,${horizontalSweep} ${
        x1 + (horizontalSweep ? -releaseNodeRadiusWithMargin : releaseNodeRadiusWithMargin)
      },${y2}`,
      `L ${x2 - releaseNodeRadiusWithMargin},${y2}`,
    ].join(' ');
  }

  public openReleaseDetails(releaseNode: ReleaseNode): void {
    const release = this.releases.find((r) => r.id === releaseNode.id) ?? null;
    this._selectedRelease.next(release);
  }

  public closeReleaseDetails(): void {
    this._selectedRelease.next(null);
  }

  private getAllReleases(): void {
    this.releaseService
      .getAllReleases()
      .pipe(
        map((record) => Object.values(record).flat()),
        tap((releases) => (this.releases = releases)),
        tap((releases) => {
          if (releases.length === 0) {
            this.toastService.error('No releases found.');
          }
          this.checkReleaseGraphLoading();
        }),
        map((releases) => this.nodeService.structureReleaseData(releases)),
        tap((sortedGroups) => this.buildReleaseGraph(sortedGroups)),
        catchError((error) => {
          console.error('Failed to load releases:', error);
          this.releaseNodes = [];
          this.releaseLinks = [];
          this.toastService.error('Failed to load releases. Please try again later.');
          this.checkReleaseGraphLoading();
          return of([]);
        }),
      )
      .subscribe();
  }

  private buildReleaseGraph(sortedGroups: Map<string, ReleaseNode[]>[]): void {
    const releaseNodeMap = this.nodeService.calculateReleaseCoordinates(sortedGroups);
    this.releaseNodes = this.nodeService.assignReleaseColors(releaseNodeMap);
    this.releaseLinks = this.linkService.createLinks(sortedGroups);
    this.checkReleaseGraphLoading();
  }

  private centerGraph(): void {
    if (!this.svgElement || !this.svgElement.nativeElement || this.releaseNodes.length === 0) return;
    this.viewBox = this.calculateViewBox(this.releaseNodes);
  }

  private calculateViewBox(nodes: ReleaseNode[]): string {
    const svg = this.svgElement.nativeElement;
    const W = svg.clientWidth;
    const H = svg.clientHeight;

    const xs = nodes.map((n) => n.position.x);
    const ys = nodes.map((n) => n.position.y);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    const graphH = maxY - minY;

    const contentHeightProportion = 0.65;
    const targetHeight = H * contentHeightProportion;
    this.scale = targetHeight / Math.max(graphH, 1);
    const scaledGraphH = graphH * this.scale;
    const topPadding = (H - scaledGraphH) / 2;
    this.translateY = -minY * this.scale + topPadding;

    this.maxTranslateX = -minX * this.scale + W * 0.25;
    this.minTranslateX = W - maxX * this.scale - W * 0.5;

    this.translateX = this.minTranslateX + W * 0.3;

    return `0 0 ${W} ${H}`;
  }

  private checkReleaseGraphLoading(): void {
    if (this.isLoading) {
      this.isLoading = false;

      setTimeout(() => {
        this.centerGraph();
      }, 50);
    }
  }
}
