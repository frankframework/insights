import { Component, ElementRef, OnInit, ViewChild, HostListener } from '@angular/core';
import { ReleaseService } from '../../services/release.service';
import { catchError, map, of, tap } from 'rxjs';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLink, ReleaseLinkService } from './release-link.service';

@Component({
  selector: 'app-release-graph',
  standalone: true,
  templateUrl: './release-graph.component.html',
  styleUrls: ['./release-graph.component.scss'],
})
export class ReleaseGraphComponent implements OnInit {
  public static readonly GITHUB_MASTER_BRANCH = 'master';

  @ViewChild('svgElement', { static: true }) svgElement!: ElementRef<SVGSVGElement>;

  public releaseNodes: ReleaseNode[] = [];
  public releaseLinks: ReleaseLink[] = [];
  public scale = 1;
  public translateX = 0;
  public translateY = 0;
  public viewBox = '0 0 0 0';

  private minTranslateX = 0;
  private maxTranslateX = 0;

  constructor(
    private releaseService: ReleaseService,
    private nodeService: ReleaseNodeService,
    private linkService: ReleaseLinkService,
  ) {}

  @HostListener('window:resize')
  onResize(): void {
    this.centerGraph();
  }

  ngOnInit(): void {
    this.getAllReleases();
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

    // Determine whether the path angle bends counterclockwise or clockwise
    const horizontalSweep = x2 > x1 ? 0 : 1;

    return [
      `M ${x1},${y1 + releaseNodeRadiusWithMargin}`,
      `L ${x1},${cornerY}`,
      `A ${releaseNodeRadiusWithMargin},${releaseNodeRadiusWithMargin} 0 0,${horizontalSweep} ${x1 + (horizontalSweep ? -releaseNodeRadiusWithMargin : releaseNodeRadiusWithMargin)},${y2}`,
      `L ${x2 - releaseNodeRadiusWithMargin},${y2}`,
    ].join(' ');
  }

  private getAllReleases(): void {
    this.releaseService
      .getAllReleases()
      .pipe(
        map((record) => Object.values(record).flat()),
        map((releases) => this.nodeService.sortReleases(releases)),
        tap((sortedGroups) => this.buildReleaseGraph(sortedGroups)),
        catchError((error) => {
          console.error('Failed to load releases:', error);
          return of([]);
        }),
      )
      .subscribe();
  }

  private buildReleaseGraph(sortedGroups: Map<string, ReleaseNode[]>[]): void {
    const releaseNodeMap = this.nodeService.calculateReleaseCoordinates(sortedGroups);
    this.releaseNodes = this.nodeService.assignReleaseColors(releaseNodeMap);
    this.releaseLinks = this.linkService.createLinks(sortedGroups);

    setTimeout(() => this.centerGraph(), 0);
  }

  private centerGraph(): void {
    if (!this.svgElement || this.releaseNodes.length === 0) return;
    this.viewBox = this.calculateViewBox(this.releaseNodes);
  }

  private calculateViewBox(nodes: ReleaseNode[]): string {
    const svg = this.svgElement.nativeElement;
    const W = svg.clientWidth;
    const H = svg.clientHeight;

    const xs = nodes.map((n) => n.position.x);
    const ys = nodes.map((n) => n.position.y);
    const minX = Math.min(...xs),
      maxX = Math.max(...xs);
    const minY = Math.min(...ys),
      maxY = Math.max(...ys);
    const graphW = maxX - minX;
    const graphH = maxY - minY;

    this.scale = (H * 0.5) / graphH;
    this.translateY = (H * 0.15) / this.scale - minY;

    const graph85X = minX + 0.85 * graphW;

    const centerGraphX = W / 2 / this.scale;

    const padPx = W * 0.25;
    const padGraph = padPx / this.scale;

    const initialTx = centerGraphX - graph85X;

    this.maxTranslateX = padGraph / 2 - minX;
    this.minTranslateX = W / this.scale - padGraph / 2 - maxX;

    this.translateX = Math.min(this.maxTranslateX, Math.max(this.minTranslateX, initialTx));

    return `0 0 ${W} ${H}`;
  }
}
