import { Component, ElementRef, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { Release, ReleaseService } from '../../services/release.service';
import { catchError, map, of, tap } from 'rxjs';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLink, ReleaseLinkService, SkipNode } from './release-link.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ReleaseCatalogusComponent } from './release-catalogus/release-catalogus.component';
import { ReleaseSkippedVersions } from './release-skipped-versions/release-skipped-versions';

@Component({
  selector: 'app-release-graph',
  standalone: true,
  templateUrl: './release-graph.component.html',
  styleUrls: ['./release-graph.component.scss'],
  imports: [LoaderComponent, ReleaseCatalogusComponent, ReleaseSkippedVersions],
})
export class ReleaseGraphComponent implements OnInit, OnDestroy {
  private static readonly RELEASE_GRAPH_NAVIGATION_PADDING: number = 50;

  @ViewChild('svgElement') svgElement!: ElementRef<SVGSVGElement>;

  public releaseNodes: ReleaseNode[] = [];
  public allLinks: ReleaseLink[] = [];
  public branchLabels: { label: string; y: number; x: number }[] = [];
  public stickyBranchLabels: { label: string; screenY: number }[] = [];
  public skipNodes: SkipNode[] = [];
  public dataForSkipModal: SkipNode | null = null;

  public isLoading = true;
  public releases: Release[] = [];
  public scale = 1;
  public translateX = 0;
  public translateY = 0;
  public viewBox = '0 0 0 0';

  public isDragging = false;
  private lastPositionX = 0;
  private minTranslateX = 0;
  private maxTranslateX = 0;
  private routerSubscription!: Subscription;
  private touchStartX = 0;
  private touchStartY = 0;
  private isTouchDragging = false;

  private releaseService = inject(ReleaseService);
  private nodeService = inject(ReleaseNodeService);
  private linkService = inject(ReleaseLinkService);
  private router = inject(Router);

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

  public onMouseDown(event: MouseEvent): void {
    event.preventDefault();
    this.isDragging = true;
    this.lastPositionX = event.clientX;
    this.svgElement.nativeElement.style.cursor = 'grabbing';
  }

  public onMouseUp(): void {
    this.isDragging = false;
    this.svgElement.nativeElement.style.cursor = 'grab';
  }

  public onMouseMove(event: MouseEvent): void {
    if (!this.isDragging) return;
    event.preventDefault();
    const deltaX = event.clientX - this.lastPositionX;
    this.lastPositionX = event.clientX;
    const newTranslateX = this.translateX + deltaX;
    this.translateX = Math.max(this.minTranslateX, Math.min(this.maxTranslateX, newTranslateX));
    this.updateStickyBranchLabels();
  }

  public onTouchStart(event: TouchEvent): void {
    event.preventDefault();
    if (event.touches.length === 1) {
      this.isDragging = true;
      this.isTouchDragging = false;
      this.lastPositionX = event.touches[0].clientX;
      this.touchStartX = event.touches[0].clientX;
      this.touchStartY = event.touches[0].clientY;
    }
  }

  public onTouchEnd(): void {
    this.isDragging = false;
    this.isTouchDragging = false;
  }

  public onTouchMove(event: TouchEvent): void {
    if (!this.isDragging || event.touches.length !== 1) return;
    event.preventDefault();
    const touch = event.touches[0];
    const deltaX = touch.clientX - this.lastPositionX;

    const totalDeltaX = Math.abs(touch.clientX - this.touchStartX);
    const totalDeltaY = Math.abs(touch.clientY - this.touchStartY);
    if (totalDeltaX > 10 || totalDeltaY > 10) {
      this.isTouchDragging = true;
    }

    this.lastPositionX = touch.clientX;
    const newTranslateX = this.translateX + deltaX;
    this.translateX = Math.max(this.minTranslateX, Math.min(this.maxTranslateX, newTranslateX));
    this.updateStickyBranchLabels();
  }

  public onNodeTouchEnd(event: TouchEvent, releaseNodeId: string): void {
    event.stopPropagation();
    if (!this.isTouchDragging) {
      this.openReleaseNodeDetails(releaseNodeId);
    }
  }

  public onSkipNodeTouchEnd(event: TouchEvent, skipNodeId: string): void {
    event.stopPropagation();
    if (!this.isTouchDragging) {
      this.openSkipNodeModal(skipNodeId);
    }
  }

  public onWheel(event: WheelEvent): void {
    event.preventDefault();
    const delta = (event.deltaX === 0 ? event.deltaY : event.deltaX) / this.scale;
    const newTranslateX = this.translateX - delta;
    this.translateX = Math.min(this.maxTranslateX, Math.max(this.minTranslateX, newTranslateX));
    this.updateStickyBranchLabels();
  }

  public getCustomPath(link: ReleaseLink): string {
    const source = this.findNodeById(link.source);
    const target = this.findNodeById(link.target);
    if (!source || !target) return '';

    const isSkipLink = link.isGap || link.isFadeIn;
    const releaseNodeRadiusWithMargin = isSkipLink ? 10 : 25;

    if (link.isGap || link.isFadeIn) {
      const [x1, y1] = [source.position.x, source.position.y];
      const [x2, y2] = [target.position.x, target.position.y];
      const startMargin = link.isFadeIn ? 0 : releaseNodeRadiusWithMargin;
      return `M ${x1 + startMargin},${y1} L ${x2 - releaseNodeRadiusWithMargin},${y2}`;
    }

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

  public openReleaseNodeDetails(releaseNodeId: string): void {
    this.router.navigate(['/graph', releaseNodeId]);
  }

  public openSkipNodeModal(skipNodeId: string): void {
    const skipNode = this.skipNodes.find((s) => s.id === skipNodeId);
    if (skipNode) {
      this.dataForSkipModal = skipNode;
    }
  }

  public closeSkipNodeModal(): void {
    this.dataForSkipModal = null;
  }

  public onSkippedVersionClick(version: string): void {
    this.closeSkipNodeModal();
    const release = this.releases.find((r) => r.name === version || `v${r.name}` === version);
    if (release) {
      this.router.navigate(['/graph', release.id]);
    }
  }

  private findNodeById(id: string): ReleaseNode | undefined {
    if (id.startsWith('start-node-') && this.releaseNodes.length > 0) {
      const firstNode = this.releaseNodes[0];
      const hasInitialSkip = this.skipNodes.some((s) => s.id.startsWith('skip-initial-'));
      const startDistance = hasInitialSkip ? 450 : 350;

      if (firstNode) {
        return {
          ...firstNode,
          id: id,
          position: { x: firstNode.position.x - startDistance, y: firstNode.position.y },
        };
      }
    }

    const skipNode = this.skipNodes.find((s) => s.id === id);
    if (skipNode) {
      return {
        id: skipNode.id,
        label: skipNode.label,
        position: { x: skipNode.x, y: skipNode.y },
        color: '#ccc',
        branch: 'skip',
        publishedAt: new Date(),
      };
    }

    return this.releaseNodes.find((n) => n.id === id);
  }

  private getAllReleases(): void {
    this.releaseService
      .getAllReleases()
      .pipe(
        map((record) => Object.values(record).flat()),
        tap((releases) => (this.releases = releases)),
        tap((releases) => {
          if (releases.length === 0) {
            console.error('No releases found.');
          }
          this.checkReleaseGraphLoading();
        }),
        map((releases) => this.nodeService.structureReleaseData(releases)),
        tap((sortedGroups) => this.buildReleaseGraph(sortedGroups)),
        catchError((error) => {
          console.error('Failed to load releases:', error);
          this.releaseNodes = [];
          this.allLinks = [];
          this.checkReleaseGraphLoading();
          return of([]);
        }),
      )
      .subscribe();
  }

  private buildReleaseGraph(sortedGroups: Map<string, ReleaseNode[]>[]): void {
    const releaseNodeMap = this.nodeService.calculateReleaseCoordinates(sortedGroups);
    this.releaseNodes = this.nodeService.assignReleaseColors(releaseNodeMap);

    this.skipNodes = this.linkService.createSkipNodes(sortedGroups, this.releases);
    const masterNodes = releaseNodeMap.get('master') ?? [];
    const skipNodeLinks = this.linkService.createSkipNodeLinks(this.skipNodes, masterNodes);

    this.allLinks = [...this.linkService.createLinks(sortedGroups, this.skipNodes), ...skipNodeLinks];
    this.branchLabels = this.createBranchLabels(releaseNodeMap, this.releases);

    this.checkReleaseGraphLoading();
  }

  private createBranchLabels(
    releaseNodeMap: Map<string, ReleaseNode[]>,
    releases: Release[],
  ): { label: string; y: number; x: number }[] {
    const labels: { label: string; y: number; x: number }[] = [];
    const allNodes = [...releaseNodeMap.values()].flat();
    const labelX = Math.min(...allNodes.map((n) => n.position.x)) - 550;
    const nodesByY = this.groupNodesByYPosition(allNodes);
    const sortedYPositions = [...nodesByY.keys()].sort((a, b) => a - b);

    for (const yPosition of sortedYPositions) {
      const branchLabel = this.determineBranchLabel(yPosition, nodesByY.get(yPosition)!, releases);
      this.addUniqueBranchLabel(labels, branchLabel, yPosition, labelX);
    }

    return labels;
  }

  private groupNodesByYPosition(allNodes: ReleaseNode[]): Map<number, ReleaseNode[]> {
    const nodesByY = new Map<number, ReleaseNode[]>();
    for (const node of allNodes) {
      if (!nodesByY.has(node.position.y)) {
        nodesByY.set(node.position.y, []);
      }
      nodesByY.get(node.position.y)!.push(node);
    }
    return nodesByY;
  }

  private determineBranchLabel(yPosition: number, nodesAtY: ReleaseNode[], releases: Release[]): string {
    return yPosition === 0 ? this.getMasterBranchLabel(nodesAtY, releases) : this.getSubBranchLabel(nodesAtY, releases);
  }

  private getMasterBranchLabel(nodesAtY: ReleaseNode[], releases: Release[]): string {
    const masterNodes = nodesAtY.filter((node) => !node.originalBranch);
    if (masterNodes.length > 0) {
      const masterRelease = releases.find((r) => r.id === masterNodes[0].id);
      return masterRelease?.branch?.name || 'master';
    }
    return 'master';
  }

  private getSubBranchLabel(nodesAtY: ReleaseNode[], releases: Release[]): string {
    const branchCounts = new Map<string, number>();

    for (const node of nodesAtY) {
      const release = releases.find((r) => r.id === node.id);
      const branchName = release?.branch?.name || 'unknown';
      branchCounts.set(branchName, (branchCounts.get(branchName) || 0) + 1);
    }

    let maxCount = 0;
    let branchLabel = 'unknown';
    for (const [branchName, count] of branchCounts.entries()) {
      if (count > maxCount) {
        maxCount = count;
        branchLabel = branchName;
      }
    }
    return branchLabel;
  }

  private addUniqueBranchLabel(
    labels: { label: string; y: number; x: number }[],
    branchLabel: string,
    yPosition: number,
    labelX: number,
  ): void {
    const existingLabel = labels.find((l) => l.y === yPosition);
    if (!existingLabel) {
      labels.push({
        label: branchLabel,
        y: yPosition,
        x: labelX,
      });
    }
  }

  private centerGraph(): void {
    if (!this.svgElement?.nativeElement || this.releaseNodes.length === 0) return;
    this.viewBox = this.calculateViewBox(this.releaseNodes);
    this.updateStickyBranchLabels();
  }

  private updateStickyBranchLabels(): void {
    if (!this.svgElement?.nativeElement) return;

    this.stickyBranchLabels = this.branchLabels.map((label) => {
      const svgY = label.y * this.scale + this.translateY;
      const svg = this.svgElement.nativeElement;
      const svgRect = svg.getBoundingClientRect();
      const screenY = svgRect.top + svgY;

      return {
        label: label.label,
        screenY: screenY,
      };
    });
  }

  private calculateViewBox(nodes: ReleaseNode[]): string {
    const svg = this.svgElement.nativeElement;
    const W = svg.clientWidth;
    const H = svg.clientHeight;

    const allCoordinates: { x: number; y: number }[] = [];

    allCoordinates.push(...nodes.map((n) => ({ x: n.position.x, y: n.position.y })));

    for (const link of this.allLinks) {
      const source = this.findNodeById(link.source);
      const target = this.findNodeById(link.target);
      if (source && target) {
        allCoordinates.push(
          { x: source.position.x, y: source.position.y },
          { x: target.position.x, y: target.position.y },
        );
      }
    }

    const xs = allCoordinates.map((coord) => coord.x);
    const ys = allCoordinates.map((coord) => coord.y);
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
    this.translateY = -minY * this.scale + topPadding + ReleaseGraphComponent.RELEASE_GRAPH_NAVIGATION_PADDING;

    this.maxTranslateX = -minX * this.scale + W * 0.2;
    this.minTranslateX = W - maxX * this.scale - W * 0.45;
    this.translateX = this.minTranslateX + W * 0.1;

    return `0 0 ${W} ${H}`;
  }

  private checkReleaseGraphLoading(): void {
    if (this.isLoading) {
      this.isLoading = false;
      setTimeout(() => this.centerGraph(), 50);
    }
  }
}
