import {
  Component,
  ElementRef,
  OnInit,
  OnDestroy,
  ViewChild,
  inject,
  AfterViewInit,
  ChangeDetectionStrategy,
} from '@angular/core';
import { Release, ReleaseService } from '../../services/release.service';
import { catchError, map, of } from 'rxjs';
import { ReleaseNode, ReleaseNodeService, QuarterMarker } from './release-node.service';
import { ReleaseLink, ReleaseLinkService, SkipNode } from './release-link.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { ActivatedRoute, NavigationEnd, Params, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ReleaseCatalogusComponent } from './release-catalogus/release-catalogus.component';
import { ReleaseSkippedVersions } from './release-skipped-versions/release-skipped-versions';
import { AuthService } from '../../services/auth.service';
import { GraphStateService } from '../../services/graph-state.service';
import { PillButtonComponent } from '../../components/pill-button/pill-button.component';
import {
  isVersionInRanges,
  mergeVersionRanges,
  parseVersionRanges,
  serializeVersionRanges,
  VersionRange,
} from '../../pipes/release-range';

export interface LifecyclePhase {
  type: 'supported';
  startX: number;
  endX: number;
  color: string;
  stroke: string;
}

export interface BranchLifecycle {
  branchLabel: string;
  y: number;
  phases: LifecyclePhase[];
}

@Component({
  selector: 'app-release-graph',
  standalone: true,
  templateUrl: './release-graph.component.html',
  styleUrls: ['./release-graph.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [LoaderComponent, ReleaseCatalogusComponent, ReleaseSkippedVersions, PillButtonComponent, RouterLink],
})
export class ReleaseGraphComponent implements OnInit, OnDestroy, AfterViewInit {
  private static readonly MAX_GRAPH_NAVIGATION_PADDING: number = 55;
  private static readonly NAV_PADDING_FULL_ROWS: number = 6;
  private static readonly NAV_PADDING_ZERO_ROWS: number = 20;
  private static readonly HEADER_HEIGHT_PX: number = 90;
  private static readonly QUARTER_LABEL_FONT_SIZE: number = 14;
  private static readonly QUARTER_LINE_GAP_PX: number = 12;
  private static readonly SVG_LINE_OVERFLOW_PX: number = 100;
  private static readonly SKIP_RELEASE_NODE_BEGIN: string = 'skip-initial-';
  private static readonly GRAPH_END_PADDING_PROPORTION: number = 0.05;

  private static readonly MAX_GRAPH_SCALE: number = 2;
  private static readonly GRAPH_HEIGHT_FILL: number = 0.8;
  private static readonly SCROLL_MARGIN_LEFT: number = 0.2;
  private static readonly LATEST_RELEASE_OFFSET: number = 0.35;

  @ViewChild('svgElement') svgElement!: ElementRef<SVGSVGElement>;

  public releaseNodes: ReleaseNode[] = [];
  public allLinks: ReleaseLink[] = [];
  public branchLabels: { label: string; y: number; x: number }[] = [];
  public stickyBranchLabels: { label: string; screenY: number }[] = [];
  public skipNodes: SkipNode[] = [];
  public dataForSkipModal: SkipNode | null = null;
  public quarterMarkers: QuarterMarker[] = [];
  public branchLifecycles: BranchLifecycle[] = [];
  public currentTimeX = 0;
  public svgLineTopY = -90;
  public svgLineBottomY = 1000;
  public svgLabelY = -105;
  public svgChevronY = -90;
  public showNotFoundError = false;
  public showNightlies = false;
  public extendedSupportLevel = 0;
  public releaseRanges: VersionRange[] = [];
  public rangeMessage: string | null = null;
  public defaultRange = '';

  public isLoading = true;
  public releases: Release[] = [];
  public scale = 1;
  public translateX = 0;
  public translateY = 0;
  public viewBox = '0 0 0 0';
  public isDragging = false;

  protected authService = inject(AuthService);

  private rangeParameter: string | null = null;
  private defaultRanges: VersionRange[] = [];
  private lastPositionX = 0;
  private minTranslateX = 0;
  private maxTranslateX = 0;
  private routerSubscription!: Subscription;
  private touchStartX = 0;
  private touchStartY = 0;
  private isTouchDragging = false;

  private wheelListener: ((event: WheelEvent) => void) | null = null;
  private touchStartListener: ((event: TouchEvent) => void) | null = null;
  private touchMoveListener: ((event: TouchEvent) => void) | null = null;
  private svgReadyObserver: ResizeObserver | null = null;

  private releaseService = inject(ReleaseService);
  private nodeService = inject(ReleaseNodeService);
  private linkService = inject(ReleaseLinkService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private graphStateService = inject(GraphStateService);

  public get currentRange(): string {
    return serializeVersionRanges(this.releaseRanges);
  }

  public get isDefaultRange(): boolean {
    return this.currentRange === this.defaultRange;
  }

  public get visibleReleaseNodes(): ReleaseNode[] {
    if (this.showNightlies) {
      return this.releaseNodes;
    }

    return this.releaseNodes.filter((node) => !this.isNightlyNode(node));
  }

  public get visibleLinks(): ReleaseLink[] {
    if (this.showNightlies) {
      return this.allLinks;
    }

    return this.allLinks.filter((link) => {
      const sourceNode = this.findNodeById(link.source);
      const targetNode = this.findNodeById(link.target);

      if (!sourceNode || !targetNode) return true;

      const isSourceHidden = this.isNightlyNode(sourceNode);
      const isTargetHidden = this.isNightlyNode(targetNode);

      return !isSourceHidden && !isTargetHidden;
    });
  }

  ngOnInit(): void {
    this.isLoading = true;

    this.route.queryParamMap.subscribe((parameters) => {
      const previousExtendedSupportLevel = this.extendedSupportLevel;
      const previousRange = this.rangeParameter;

      this.extendedSupportLevel = GraphStateService.parseExtendedSupportLevel(parameters.get('extended'));
      this.showNightlies = parameters.has('nightly');
      this.rangeParameter = parameters.get('range');

      this.graphStateService.setExtendedSupportLevel(this.extendedSupportLevel);
      this.graphStateService.setShowNightlies(this.showNightlies);

      const rangeChanged = previousRange !== this.rangeParameter;

      if ((previousExtendedSupportLevel !== this.extendedSupportLevel || rangeChanged) && this.releases.length > 0) {
        this.applyRange();
      }
    });

    this.getAllReleases();

    this.routerSubscription = this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd && this.router.url.includes('/graph')) {
        this.waitForSvgReady(() => this.centerGraph());
      }
    });
  }

  ngAfterViewInit(): void {
    this.attachNonPassiveEventListeners();
  }

  ngOnDestroy(): void {
    this.routerSubscription?.unsubscribe();
    this.removeNonPassiveEventListeners();
    this.svgReadyObserver?.disconnect();
  }

  public toggleNightlies(): void {
    const queryParameters = { ...this.graphStateService.getGraphQueryParams() };
    if (this.showNightlies) {
      delete queryParameters['nightly'];
    } else {
      queryParameters['nightly'] = '';
    }
    this.router.navigate([], { queryParams: queryParameters, replaceUrl: true });
  }

  public onRangeRequested(ranges: VersionRange[]): void {
    this.closeSkipNodeModal();
    this.navigateWithRanges(mergeVersionRanges(this.releaseRanges, ranges));
  }

  public resetRange(): void {
    this.navigateWithRanges([]);
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

  public handleNodeTouchEnd(event: TouchEvent, releaseNode: ReleaseNode): void {
    if (releaseNode.isMiniNode) {
      return;
    }
    this.onNodeTouchEnd(event, releaseNode.id);
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

    const isMiniNode = source.isMiniNode || false;

    if (link.isGap || link.isFadeIn) {
      const [x1, y1] = [source.position.x, source.position.y];
      const [x2, y2] = [target.position.x, target.position.y];
      return `M ${x1},${y1} L ${x2},${y2}`;
    }

    const [x1, y1] = [source.position.x, source.position.y];
    const [x2, y2] = [target.position.x, target.position.y];

    if (y1 === y2) {
      return `M ${x1},${y1} L ${x2},${y2}`;
    }

    if (isMiniNode && y2 > y1) {
      const curveRadius = 20;
      const targetLeftSide = x2 - 2;
      const cornerY = y2 - curveRadius;

      return [
        `M ${x1},${y1}`,
        `L ${x1},${cornerY}`,
        `A ${curveRadius},${curveRadius} 0 0,0 ${x1 + curveRadius},${y2}`,
        `L ${targetLeftSide},${y2}`,
      ].join(' ');
    }

    const verticalDirection = y2 > y1 ? 1 : -1;
    const cornerY = y2 - verticalDirection;
    const horizontalSweep = x2 > x1 ? 0 : 1;

    return [`M ${x1},${y1}`, `L ${x1},${cornerY}`, `A 0,0 0 0,${horizontalSweep} ${x1},${y2}`, `L ${x2},${y2}`].join(
      ' ',
    );
  }

  public openReleaseNodeDetails(releaseNodeId: string): void {
    const tree = this.router.parseUrl(this.releaseNodeLink(releaseNodeId));
    tree.queryParams = { ...tree.queryParams, ...this.graphQueryParams() };
    this.router.navigateByUrl(tree);
  }

  public releaseNodeLink(releaseNodeId: string): string {
    const release = this.releases.find((r) => r.id === releaseNodeId);
    const identifier = (release?.tagName ?? releaseNodeId).replace(/^release\//, '');
    return `/graph/${identifier}`;
  }

  public graphQueryParams(): Params {
    return this.graphStateService.getGraphQueryParams();
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
      const queryParameters = this.graphStateService.getGraphQueryParams();
      this.router.navigate(['/graph', release.tagName.replace(/^release\//, '')], { queryParams: queryParameters });
    }
  }

  public isMajorVersionBranch(branchLabel: string): boolean {
    const match = branchLabel.match(/(\d+)\.(\d+)$/);
    if (!match) return false;
    const minor = Number.parseInt(match[2], 10);
    return minor === 0;
  }

  private navigateWithRanges(ranges: VersionRange[]): void {
    const queryParameters = { ...this.graphStateService.getGraphQueryParams() };
    const range = serializeVersionRanges(ranges);

    if (range) {
      queryParameters['range'] = range;
    } else {
      delete queryParameters['range'];
    }

    this.router.navigate([], { queryParams: queryParameters, replaceUrl: true });
  }

  private applyRange(): void {
    if (!this.rangeParameter?.trim()) {
      this.navigateWithRanges(this.defaultRanges);
      return;
    }

    const previousMessage = this.rangeMessage;
    const parsedRanges = parseVersionRanges(this.rangeParameter);

    this.releaseRanges = parsedRanges.ranges;
    this.rangeMessage = parsedRanges.error ?? this.describeEmptyRange(parsedRanges.ranges);
    this.graphStateService.setReleaseRanges(this.releaseRanges);

    if (this.rangeMessage) {
      this.clearReleaseGraph();
      return;
    }

    if (previousMessage) this.isLoading = true;

    this.buildReleaseGraph(this.nodeService.structureReleaseData(this.releases, this.releaseRanges));
  }

  private describeEmptyRange(ranges: VersionRange[]): string | null {
    const hasMatch = this.releases.some((release) => {
      const versionInfo = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      return versionInfo !== null && isVersionInRanges(ranges, versionInfo.major, versionInfo.minor, versionInfo.patch);
    });

    return hasMatch ? null : `No releases were found for ${serializeVersionRanges(ranges)}.`;
  }

  private clearReleaseGraph(): void {
    this.releaseNodes = [];
    this.allLinks = [];
    this.skipNodes = [];
    this.branchLabels = [];
    this.stickyBranchLabels = [];
    this.branchLifecycles = [];
    this.quarterMarkers = [];
    this.isLoading = false;
  }

  private isNightlyNode(node: ReleaseNode): boolean {
    if (node.isMiniNode) {
      return false;
    }

    if (node.position.y === 0) {
      return false;
    }
    const label = node.label.toLowerCase();
    return label.includes('nightly') || /^v?\d+\.\d+\.\d+-\d{8}\.\d{6}/.test(node.label);
  }

  private findNodeById(id: string): ReleaseNode | undefined {
    if (id.startsWith('start-node-') && this.releaseNodes.length > 0) {
      const firstNode = this.releaseNodes[0];
      const hasInitialSkip = this.skipNodes.some((s) => s.id.startsWith(ReleaseGraphComponent.SKIP_RELEASE_NODE_BEGIN));

      let startDistance = 300;
      if (hasInitialSkip) {
        const initialSkipNode = this.skipNodes.find((s) =>
          s.id.startsWith(ReleaseGraphComponent.SKIP_RELEASE_NODE_BEGIN),
        );
        if (initialSkipNode) {
          startDistance = firstNode.position.x - initialSkipNode.x + startDistance;
        }
      } else {
        startDistance = Math.min(firstNode.position.x * 0.8, startDistance);
      }

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

    const node = this.releaseNodes.find((n) => n.id === id);
    if (node) {
      return node;
    }

    return undefined;
  }

  private getAllReleases(): void {
    this.releaseService
      .getAllReleases()
      .pipe(
        map((record) => Object.values(record).flat()),
        catchError(() => {
          this.showNotFoundError = true;
          this.releaseNodes = [];
          this.allLinks = [];
          this.checkReleaseGraphLoading();
          return of([]);
        }),
      )
      .subscribe((releases) => {
        this.releases = releases;
        if (releases.length === 0) {
          this.showNotFoundError = true;
          this.checkReleaseGraphLoading();
          return;
        }
        this.defaultRanges = this.nodeService.getDefaultRanges(releases);
        this.defaultRange = serializeVersionRanges(this.defaultRanges);
        this.applyRange();
      });
  }

  private buildReleaseGraph(sortedGroups: Map<string, ReleaseNode[]>[]): void {
    const releaseNodeMap = this.nodeService.calculateReleaseCoordinates(sortedGroups);
    this.nodeService.assignReleaseColors(releaseNodeMap);

    this.releaseNodes = [];
    for (const [, nodes] of releaseNodeMap.entries()) {
      const spacedNodes = this.nodeService.applyMinimumSpacing(nodes);
      this.releaseNodes.push(...spacedNodes);
    }

    this.skipNodes = this.linkService.createSkipNodes(sortedGroups, this.releases);

    this.updateSkipNodePositions();

    const masterNodes = releaseNodeMap.get('master') ?? [];
    const skipNodeLinks = this.linkService.createSkipNodeLinks(this.skipNodes, masterNodes);

    this.allLinks = [...this.linkService.createLinks(sortedGroups, this.skipNodes), ...skipNodeLinks];
    this.branchLabels = this.createBranchLabels(releaseNodeMap, this.releases);
    this.branchLifecycles = this.calculateBranchLifecycles(releaseNodeMap);

    this.quarterMarkers = this.extendQuarterMarkersToLifecycleEnd();

    if (this.nodeService.timelineScale) {
      this.currentTimeX = this.calculateXPositionFromDate(new Date(), this.nodeService.timelineScale);
    }

    this.checkReleaseGraphLoading();
  }

  private extendQuarterMarkersToLifecycleEnd(): QuarterMarker[] {
    const baseMarkers = this.nodeService.timelineScale?.quarters ?? [];
    if (baseMarkers.length === 0) return baseMarkers;

    const lastMarker = baseMarkers.at(-1);
    const maxLifecycleEndX = this.getMaxLifecycleEndX();
    if (maxLifecycleEndX === 0 || lastMarker!.x >= maxLifecycleEndX) {
      return baseMarkers;
    }

    const additionalMarkers = this.generateAdditionalQuarters(lastMarker!, maxLifecycleEndX);
    return [...baseMarkers, ...additionalMarkers];
  }

  private getMaxLifecycleEndX(): number {
    let maxEndX = 0;
    for (const lifecycle of this.branchLifecycles) {
      for (const phase of lifecycle.phases) {
        maxEndX = Math.max(maxEndX, phase.endX);
      }
    }
    return maxEndX;
  }

  private generateAdditionalQuarters(lastMarker: QuarterMarker, maxEndX: number): QuarterMarker[] {
    if (!this.nodeService.timelineScale) return [];

    const markers: QuarterMarker[] = [];
    let currentDate = new Date(lastMarker.date);
    currentDate.setMonth(currentDate.getMonth() + 3);
    let lastAddedX = lastMarker.x;

    while (true) {
      const x = this.calculateXFromDate(currentDate);
      if (x > maxEndX) break;

      markers.push(this.createQuarterMarker(currentDate, x, true));
      lastAddedX = x;
      currentDate.setMonth(currentDate.getMonth() + 3);
    }

    if (markers.length > 0 && lastAddedX < maxEndX) {
      markers.push(this.createQuarterMarker(currentDate, this.calculateXFromDate(currentDate), false));
    }

    return markers;
  }

  private createQuarterMarker(date: Date, x: number, withLabel: boolean): QuarterMarker {
    const quarter = Math.floor(date.getMonth() / 3) + 1;
    return {
      label: withLabel ? `Q${quarter} ${date.getFullYear()}` : '',
      date: new Date(date),
      x,
      labelX: x + 100,
      year: date.getFullYear(),
      quarter,
    };
  }

  private calculateXFromDate(date: Date): number {
    if (!this.nodeService.timelineScale) return 0;
    const daysSinceStart =
      (date.getTime() - this.nodeService.timelineScale.startDate.getTime()) / (1000 * 60 * 60 * 24);
    return daysSinceStart * this.nodeService.timelineScale.pixelsPerDay;
  }

  private updateSkipNodePositions(): void {
    for (const skipNode of this.skipNodes) {
      const isInitial = skipNode.id.startsWith(ReleaseGraphComponent.SKIP_RELEASE_NODE_BEGIN);

      if (isInitial) {
        const firstNode = this.releaseNodes[0];
        if (firstNode) {
          skipNode.x = firstNode.position.x / 2;
          skipNode.y = firstNode.position.y;
        }
      } else {
        const match = skipNode.id.match(/^skip-(.+)-(.+)$/);
        if (match) {
          const sourceId = match[1];
          const targetId = match[2];

          const sourceNode = this.findNodeById(sourceId);
          const targetNode = this.findNodeById(targetId);

          if (sourceNode && targetNode) {
            skipNode.x = (sourceNode.position.x + targetNode.position.x) / 2;
            skipNode.y = sourceNode.position.y;
          }
        }
      }
    }
  }

  private createBranchLabels(
    releaseNodeMap: Map<string, ReleaseNode[]>,
    releases: Release[],
  ): { label: string; y: number; x: number }[] {
    const labels: { label: string; y: number; x: number }[] = [];
    const allNodes = [...releaseNodeMap.values()].flat();
    const labelX = Math.min(...allNodes.map((n) => n.position.x)) - 550;
    const nodesByY = this.groupNodesByYPosition(allNodes);
    const sortedYPositions = [...nodesByY.keys()].toSorted((a, b) => a - b);

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
    return yPosition === 0 ? this.getMasterBranchLabel(nodesAtY, releases) : this.getBranchLabel(nodesAtY, releases);
  }

  private getMasterBranchLabel(nodesAtY: ReleaseNode[], releases: Release[]): string {
    const masterNodes = nodesAtY.filter((node) => !node.originalBranch);
    if (masterNodes.length > 0) {
      const masterRelease = releases.find((r) => r.id === masterNodes[0].id);
      return masterRelease?.branch?.name || 'master';
    }
    return 'master';
  }

  private getBranchLabel(nodesAtY: ReleaseNode[], releases: Release[]): string {
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

    const allCoordinates: { x: number; y: number }[] = nodes.map((n) => ({ x: n.position.x, y: n.position.y }));
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

    const releaseXs = allCoordinates.map((coord) => coord.x);
    const contentXs = [...releaseXs, ...this.collectLifecyclePhaseXPositions()];
    const ys = allCoordinates.map((coord) => coord.y);
    const latestReleaseX = Math.max(...releaseXs);
    const minX = Math.min(...contentXs);
    const maxX = Math.max(...contentXs, this.lastQuarterMarkerX());
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    const graphH = maxY - minY;

    const navPadding = this.calculateNavPadding(ys);

    const availableH = H - ReleaseGraphComponent.HEADER_HEIGHT_PX - navPadding;
    const targetHeight = availableH * ReleaseGraphComponent.GRAPH_HEIGHT_FILL;

    this.scale = Math.min(targetHeight / Math.max(graphH, 1), ReleaseGraphComponent.MAX_GRAPH_SCALE);
    const scaledGraphH = graphH * this.scale;
    const topPadding = (availableH - scaledGraphH) / 2 + ReleaseGraphComponent.HEADER_HEIGHT_PX;
    this.translateY = -minY * this.scale + topPadding + navPadding;

    this.maxTranslateX = -minX * this.scale + W * ReleaseGraphComponent.SCROLL_MARGIN_LEFT;
    this.minTranslateX = W - maxX * this.scale - W * ReleaseGraphComponent.GRAPH_END_PADDING_PROPORTION;

    const latestReleaseTranslateX = W - latestReleaseX * this.scale - W * ReleaseGraphComponent.LATEST_RELEASE_OFFSET;
    this.translateX = Math.max(this.minTranslateX, Math.min(this.maxTranslateX, latestReleaseTranslateX));

    const headerBottom = ReleaseGraphComponent.HEADER_HEIGHT_PX;
    const labelFontSize = ReleaseGraphComponent.QUARTER_LABEL_FONT_SIZE;
    const lineGap = ReleaseGraphComponent.QUARTER_LINE_GAP_PX;
    const lineOverflow = ReleaseGraphComponent.SVG_LINE_OVERFLOW_PX;

    this.svgLabelY = (headerBottom - this.translateY) / this.scale;
    this.svgLineTopY = (headerBottom + lineGap - this.translateY) / this.scale + labelFontSize;
    this.svgChevronY = (headerBottom - this.translateY) / this.scale;
    this.svgLineBottomY = (H + lineOverflow - this.translateY) / this.scale;

    return `0 0 ${W} ${H}`;
  }

  private calculateNavPadding(ys: number[]): number {
    const rowCount = new Set(ys).size;
    const t = Math.min(
      1,
      Math.max(
        0,
        (rowCount - ReleaseGraphComponent.NAV_PADDING_FULL_ROWS) /
          (ReleaseGraphComponent.NAV_PADDING_ZERO_ROWS - ReleaseGraphComponent.NAV_PADDING_FULL_ROWS),
      ),
    );
    return Math.round(ReleaseGraphComponent.MAX_GRAPH_NAVIGATION_PADDING * (1 - t));
  }

  private collectLifecyclePhaseXPositions(): number[] {
    return this.branchLifecycles.flatMap((lifecycle) =>
      lifecycle.phases.flatMap((phase) => [phase.startX, phase.endX]),
    );
  }

  private lastQuarterMarkerX(): number {
    return this.quarterMarkers.at(-1)?.x ?? Number.NEGATIVE_INFINITY;
  }

  private calculateBranchLifecycles(releaseNodeMap: Map<string, ReleaseNode[]>): BranchLifecycle[] {
    if (!this.nodeService.timelineScale) return [];

    const lifecycles: BranchLifecycle[] = [];
    const nodesByY = this.groupNodesByYPosition([...releaseNodeMap.values()].flat());
    const sortedYPositions = [...nodesByY.keys()].toSorted((a, b) => a - b);

    for (const yPosition of sortedYPositions) {
      if (yPosition === 0) continue;

      const nodesAtY = nodesByY.get(yPosition)!;
      const branchLabel = this.determineBranchLabel(yPosition, nodesAtY, this.releases);

      const allNodesInBranch = this.getAllNodesInBranch(nodesAtY);

      const sortedNodes = [...allNodesInBranch].toSorted((a, b) => a.position.x - b.position.x);

      if (sortedNodes.length === 0) continue;

      const phases = this.calculateLifecyclePhasesForBranch(sortedNodes);

      if (phases.length > 0) {
        lifecycles.push({
          branchLabel,
          y: yPosition,
          phases,
        });
      }
    }

    return lifecycles;
  }

  private getAllNodesInBranch(nodes: ReleaseNode[]): ReleaseNode[] {
    return nodes;
  }

  private identifyAnyNodeById(nodeId: string): ReleaseNode | undefined {
    return this.releaseNodes.find((n) => n.id === nodeId);
  }

  private findMajorMinorRelease(branchMajorMinor: string): Release | undefined {
    return this.releases.find((release) => {
      const releaseName = release.name.startsWith('v') ? release.name.slice(1) : release.name;
      return releaseName.startsWith(`${branchMajorMinor}.0`) && !releaseName.includes('nightly');
    });
  }

  private calculateSupportEndDate(branchStartDate: Date, totalSupportQuarters: number, isMajor: boolean): Date {
    const quartersPerExtendedWindow = isMajor ? 2 : 1;
    const extendedQuarters = quartersPerExtendedWindow * this.extendedSupportLevel;
    return this.addMonths(branchStartDate, (totalSupportQuarters + extendedQuarters) * 3);
  }

  private calculatePhaseBoundaries(
    branchStartDate: Date,
    offsetStartX: number,
    supportEndX: number,
    phaseMonths: number,
    scale: { startDate: Date; pixelsPerDay: number },
  ): { greenPhaseEndX: number; extendedPhaseEndsX: number[] } {
    if (this.extendedSupportLevel === 0) {
      const midpointX = offsetStartX + (supportEndX - offsetStartX) / 2;
      return { greenPhaseEndX: midpointX, extendedPhaseEndsX: [] };
    }

    const greenPhaseEndX = this.calculateXPositionFromDate(this.addMonths(branchStartDate, phaseMonths), scale);

    const extendedPhaseEndsX = Array.from({ length: this.extendedSupportLevel }, (_, index) =>
      this.calculateXPositionFromDate(this.addMonths(branchStartDate, phaseMonths * (index + 2)), scale),
    );

    return { greenPhaseEndX, extendedPhaseEndsX };
  }

  private addMonths(date: Date, months: number): Date {
    const shiftedDate = new Date(date);
    shiftedDate.setMonth(date.getMonth() + months);
    return shiftedDate;
  }

  private createLifecyclePhases(
    offsetStartX: number,
    greenPhaseEndX: number,
    extendedPhaseEndsX: number[],
    supportEndX: number,
    isOutdated: boolean,
  ): LifecyclePhase[] {
    const OUTDATED_FILL_COLOR = 'rgba(210, 210, 210, 0.25)';
    const OUTDATED_STROKE_COLOR = 'rgba(180, 180, 180, 0.4)';

    const phases: LifecyclePhase[] = [
      {
        type: 'supported',
        startX: offsetStartX,
        endX: greenPhaseEndX,
        color: isOutdated ? OUTDATED_FILL_COLOR : 'rgba(144, 238, 144, 0.20)',
        stroke: isOutdated ? OUTDATED_STROKE_COLOR : 'rgba(144, 238, 144, 0.4)',
      },
    ];

    let previousPhaseEndX = greenPhaseEndX;
    for (const extendedPhaseEndX of extendedPhaseEndsX) {
      phases.push({
        type: 'supported',
        startX: previousPhaseEndX,
        endX: extendedPhaseEndX,
        color: isOutdated ? OUTDATED_FILL_COLOR : 'rgba(59, 130, 246, 0.15)',
        stroke: isOutdated ? OUTDATED_STROKE_COLOR : 'rgba(59, 130, 246, 0.15)',
      });
      previousPhaseEndX = extendedPhaseEndX;
    }

    phases.push({
      type: 'supported',
      startX: previousPhaseEndX,
      endX: supportEndX,
      color: isOutdated ? OUTDATED_FILL_COLOR : 'rgba(251, 146, 60, 0.15)',
      stroke: isOutdated ? OUTDATED_STROKE_COLOR : 'rgba(251, 146, 60, 0.2)',
    });

    return phases;
  }

  private calculateLifecyclePhasesForBranch(sortedNodes: ReleaseNode[]): LifecyclePhase[] {
    if (!this.nodeService.timelineScale || sortedNodes.length === 0) return [];

    const scale = this.nodeService.timelineScale;
    const firstNode = sortedNodes[0];
    const firstVersionInfo = this.nodeService.getVersionInfo(firstNode);
    if (!firstVersionInfo) return [];

    const branchMajorMinor = `${firstVersionInfo.major}.${firstVersionInfo.minor}`;
    const majorMinorRelease = this.findMajorMinorRelease(branchMajorMinor);
    if (!majorMinorRelease) return [];

    const majorMinorNode = this.identifyAnyNodeById(majorMinorRelease.id);
    if (!majorMinorNode) return [];

    const versionInfo = this.nodeService.getVersionInfo(majorMinorNode);
    if (!versionInfo) return [];

    const totalSupportQuarters = versionInfo.type === 'major' ? 4 : 2;
    const branchStartDate = new Date(majorMinorRelease.publishedAt);
    const supportEnd = this.calculateSupportEndDate(
      branchStartDate,
      totalSupportQuarters,
      versionInfo.type === 'major',
    );

    const supportEndX = this.calculateXPositionFromDate(supportEnd, scale);
    const MINI_NODE_OFFSET = 40;
    const offsetStartX = majorMinorNode.position.x - MINI_NODE_OFFSET;

    const isOutdated = new Date() > supportEnd;
    const phaseMonths = versionInfo.type === 'major' ? 6 : 3;

    const { greenPhaseEndX, extendedPhaseEndsX } = this.calculatePhaseBoundaries(
      branchStartDate,
      offsetStartX,
      supportEndX,
      phaseMonths,
      scale,
    );

    return this.createLifecyclePhases(offsetStartX, greenPhaseEndX, extendedPhaseEndsX, supportEndX, isOutdated);
  }

  private calculateXPositionFromDate(date: Date, scale: { startDate: Date; pixelsPerDay: number }): number {
    const daysSinceStart = (date.getTime() - scale.startDate.getTime()) / (1000 * 60 * 60 * 24);
    return daysSinceStart * scale.pixelsPerDay;
  }

  private checkReleaseGraphLoading(): void {
    if (this.isLoading) {
      this.isLoading = false;
      this.waitForSvgReady(() => {
        this.centerGraph();
        this.attachNonPassiveEventListeners();
      });
    }
  }

  private waitForSvgReady(callback: () => void): void {
    const svg = this.svgElement?.nativeElement;

    if (!svg) {
      requestAnimationFrame(() => this.waitForSvgReady(callback));
      return;
    }

    if (svg.clientWidth > 0) {
      requestAnimationFrame(() => callback());
      return;
    }

    this.svgReadyObserver?.disconnect();
    this.svgReadyObserver = new ResizeObserver(() => {
      if (svg.clientWidth > 0) {
        this.svgReadyObserver!.disconnect();
        this.svgReadyObserver = null;
        requestAnimationFrame(() => callback());
      }
    });

    this.svgReadyObserver.observe(svg);
  }

  private attachNonPassiveEventListeners(): void {
    if (!this.svgElement?.nativeElement) return;

    const svg = this.svgElement.nativeElement;

    this.wheelListener = this.onWheel.bind(this);
    this.touchStartListener = this.onTouchStart.bind(this);
    this.touchMoveListener = this.onTouchMove.bind(this);

    svg.addEventListener('wheel', this.wheelListener, { passive: false });
    svg.addEventListener('touchstart', this.touchStartListener, { passive: false });
    svg.addEventListener('touchmove', this.touchMoveListener, { passive: false });
  }

  private removeNonPassiveEventListeners(): void {
    if (!this.svgElement?.nativeElement) return;

    const svg = this.svgElement.nativeElement;

    if (this.wheelListener) {
      svg.removeEventListener('wheel', this.wheelListener);
    }
    if (this.touchStartListener) {
      svg.removeEventListener('touchstart', this.touchStartListener);
    }
    if (this.touchMoveListener) {
      svg.removeEventListener('touchmove', this.touchMoveListener);
    }
  }
}
