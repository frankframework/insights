import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  OnInit,
  Signal,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LoaderComponent, ReleaseCatalogusComponent, ReleaseSkippedVersions, PillButtonComponent, RouterLink],
})
export class ReleaseGraphComponent implements OnInit, OnDestroy, AfterViewInit {
  private static readonly RELEASE_GRAPH_NAVIGATION_PADDING: number = 55;
  private static readonly HEADER_HEIGHT_PX: number = 90;
  private static readonly QUARTER_LABEL_FONT_SIZE: number = 14;
  private static readonly QUARTER_LINE_GAP_PX: number = 12;
  private static readonly SVG_LINE_OVERFLOW_PX: number = 100;
  private static readonly SKIP_RELEASE_NODE_BEGIN: string = 'skip-initial-';
  private static readonly GRAPH_END_PADDING_PROPORTION: number = 0.05;

  public readonly svgElement = viewChild<ElementRef<SVGSVGElement>>('svgElement');

  public readonly releaseNodes = signal<ReleaseNode[]>([]);
  public readonly allLinks = signal<ReleaseLink[]>([]);
  public readonly branchLabels = signal<{ label: string; y: number; x: number }[]>([]);
  public readonly stickyBranchLabels = signal<{ label: string; screenY: number }[]>([]);
  public readonly skipNodes = signal<SkipNode[]>([]);
  public readonly dataForSkipModal = signal<SkipNode | null>(null);
  public readonly quarterMarkers = signal<QuarterMarker[]>([]);
  public readonly branchLifecycles = signal<BranchLifecycle[]>([]);
  public readonly currentTimeX = signal(0);
  public readonly svgLineTopY = signal(-90);
  public readonly svgLineBottomY = signal(1000);
  public readonly svgLabelY = signal(-105);
  public readonly svgChevronY = signal(-90);
  public readonly showNotFoundError = signal(false);
  public readonly showNightlies = signal(false);
  public readonly extendedSupportLevel = signal(0);

  public readonly isLoading = signal(true);
  public readonly releases = signal<Release[]>([]);
  public readonly scale = signal(1);
  public readonly translateX = signal(0);
  public readonly translateY = signal(0);
  public readonly viewBox = signal('0 0 0 0');
  public isDragging = false;

  public readonly graphQueryParams: Signal<Params> = computed(() => this.graphStateService.graphQueryParams());

  public readonly visibleReleaseNodes: Signal<ReleaseNode[]> = computed(() => {
    const releaseNodes = this.releaseNodes();
    if (this.showNightlies()) return releaseNodes;

    return releaseNodes.filter((node) => !ReleaseGraphComponent.isNightlyNode(node));
  });

  public readonly visibleLinks: Signal<ReleaseLink[]> = computed(() => {
    const allLinks = this.allLinks();
    if (this.showNightlies()) return allLinks;

    return allLinks.filter((link) => {
      const sourceNode = this.findNodeById(link.source);
      const targetNode = this.findNodeById(link.target);

      if (!sourceNode || !targetNode) return true;

      return !ReleaseGraphComponent.isNightlyNode(sourceNode) && !ReleaseGraphComponent.isNightlyNode(targetNode);
    });
  });

  protected authService = inject(AuthService);

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
  private destroyRef = inject(DestroyRef);

  private static isNightlyNode(node: ReleaseNode): boolean {
    if (node.isMiniNode) {
      return false;
    }

    if (node.position.y === 0) {
      return false;
    }
    const label = node.label.toLowerCase();
    return label.includes('nightly') || /^v?\d+\.\d+\.\d+-\d{8}\.\d{6}/.test(node.label);
  }

  ngOnInit(): void {
    this.isLoading.set(true);
    this.getAllReleases();

    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((parameters) => {
      const previousExtendedSupportLevel = this.extendedSupportLevel();
      const extendedSupportLevel = GraphStateService.parseExtendedSupportLevel(parameters['extended']);
      const showNightlies = parameters['nightly'] !== undefined;

      this.extendedSupportLevel.set(extendedSupportLevel);
      this.showNightlies.set(showNightlies);

      this.graphStateService.setExtendedSupportLevel(extendedSupportLevel);
      this.graphStateService.setShowNightlies(showNightlies);

      const releases = this.releases();
      if (previousExtendedSupportLevel !== extendedSupportLevel && releases.length > 0) {
        this.buildReleaseGraph(this.nodeService.structureReleaseData(releases));
      }
    });

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
    const queryParameters = { ...this.graphStateService.graphQueryParams() };
    if (this.showNightlies()) {
      delete queryParameters['nightly'];
    } else {
      queryParameters['nightly'] = '';
    }
    this.router.navigate([], { queryParams: queryParameters, replaceUrl: true });
  }

  public onMouseDown(event: MouseEvent): void {
    event.preventDefault();
    this.isDragging = true;
    this.lastPositionX = event.clientX;
    this.requireSvg().style.cursor = 'grabbing';
  }

  public onMouseUp(): void {
    this.isDragging = false;
    this.requireSvg().style.cursor = 'grab';
  }

  public onMouseMove(event: MouseEvent): void {
    if (!this.isDragging) return;
    event.preventDefault();
    const deltaX = event.clientX - this.lastPositionX;
    this.lastPositionX = event.clientX;
    this.translateX.set(this.clampTranslateX(this.translateX() + deltaX));
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
    this.translateX.set(this.clampTranslateX(this.translateX() + deltaX));
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
    const delta = (event.deltaX === 0 ? event.deltaY : event.deltaX) / this.scale();
    this.translateX.set(this.clampTranslateX(this.translateX() - delta));
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
    const release = this.releases().find((r) => r.id === releaseNodeId);
    const identifier = (release?.tagName ?? releaseNodeId).replace(/^release\//, '');
    return `/graph/${identifier}`;
  }

  public openSkipNodeModal(skipNodeId: string): void {
    const skipNode = this.skipNodes().find((s) => s.id === skipNodeId);
    if (skipNode) {
      this.dataForSkipModal.set(skipNode);
    }
  }

  public closeSkipNodeModal(): void {
    this.dataForSkipModal.set(null);
  }

  public onSkippedVersionClick(version: string): void {
    this.closeSkipNodeModal();
    const release = this.releases().find((r) => r.name === version || `v${r.name}` === version);
    if (release) {
      const queryParameters = this.graphStateService.graphQueryParams();
      this.router.navigate(['/graph', release.tagName.replace(/^release\//, '')], { queryParams: queryParameters });
    }
  }

  public isMajorVersionBranch(branchLabel: string): boolean {
    const match = branchLabel.match(/(\d+)\.(\d+)$/);
    if (!match) return false;
    const minor = Number.parseInt(match[2], 10);
    return minor === 0;
  }

  private svgNative(): SVGSVGElement | undefined {
    return this.svgElement()?.nativeElement;
  }

  private requireSvg(): SVGSVGElement {
    const svg = this.svgNative();
    if (!svg) throw new Error('SVG element is not available yet');
    return svg;
  }

  private clampTranslateX(value: number): number {
    return Math.max(this.minTranslateX, Math.min(this.maxTranslateX, value));
  }

  private findNodeById(id: string): ReleaseNode | undefined {
    const releaseNodes = this.releaseNodes();
    const skipNodes = this.skipNodes();

    if (id.startsWith('start-node-') && releaseNodes.length > 0) {
      const firstNode = releaseNodes[0];
      const hasInitialSkip = skipNodes.some((s) => s.id.startsWith(ReleaseGraphComponent.SKIP_RELEASE_NODE_BEGIN));

      let startDistance = 300;
      if (hasInitialSkip) {
        const initialSkipNode = skipNodes.find((s) => s.id.startsWith(ReleaseGraphComponent.SKIP_RELEASE_NODE_BEGIN));
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

    const skipNode = skipNodes.find((s) => s.id === id);
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

    const node = releaseNodes.find((n) => n.id === id);
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
          this.showNotFoundError.set(true);
          this.releaseNodes.set([]);
          this.allLinks.set([]);
          this.checkReleaseGraphLoading();
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((releases) => {
        this.releases.set(releases);
        if (releases.length === 0) {
          this.showNotFoundError.set(true);
          this.checkReleaseGraphLoading();
          return;
        }
        const sortedGroups = this.nodeService.structureReleaseData(releases);
        this.buildReleaseGraph(sortedGroups);
      });
  }

  private buildReleaseGraph(sortedGroups: Map<string, ReleaseNode[]>[]): void {
    const releaseNodeMap = this.nodeService.calculateReleaseCoordinates(sortedGroups);
    this.nodeService.assignReleaseColors(releaseNodeMap);

    const releaseNodes: ReleaseNode[] = [];
    for (const [, nodes] of releaseNodeMap.entries()) {
      releaseNodes.push(...this.nodeService.applyMinimumSpacing(nodes));
    }
    this.releaseNodes.set(releaseNodes);

    const releases = this.releases();
    const skipNodes = this.linkService.createSkipNodes(sortedGroups, releases);
    this.skipNodes.set(skipNodes);

    this.updateSkipNodePositions();

    const masterNodes = releaseNodeMap.get('master') ?? [];
    const skipNodeLinks = this.linkService.createSkipNodeLinks(skipNodes, masterNodes);

    this.allLinks.set([...this.linkService.createLinks(sortedGroups, skipNodes), ...skipNodeLinks]);
    this.branchLabels.set(this.createBranchLabels(releaseNodeMap, releases));
    this.branchLifecycles.set(this.calculateBranchLifecycles(releaseNodeMap));

    this.quarterMarkers.set(this.extendQuarterMarkersToLifecycleEnd());

    if (this.nodeService.timelineScale) {
      this.currentTimeX.set(this.calculateXPositionFromDate(new Date(), this.nodeService.timelineScale));
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
    for (const lifecycle of this.branchLifecycles()) {
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
    for (const skipNode of this.skipNodes()) {
      const isInitial = skipNode.id.startsWith(ReleaseGraphComponent.SKIP_RELEASE_NODE_BEGIN);

      if (isInitial) {
        const firstNode = this.releaseNodes()[0];
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
    const releaseNodes = this.releaseNodes();
    if (!this.svgNative() || releaseNodes.length === 0) return;
    this.viewBox.set(this.calculateViewBox(releaseNodes));
    this.updateStickyBranchLabels();
  }

  private updateStickyBranchLabels(): void {
    const svg = this.svgNative();
    if (!svg) return;

    const scale = this.scale();
    const translateY = this.translateY();
    const svgRect = svg.getBoundingClientRect();

    this.stickyBranchLabels.set(
      this.branchLabels().map((label) => ({
        label: label.label,
        screenY: svgRect.top + label.y * scale + translateY,
      })),
    );
  }

  private calculateViewBox(nodes: ReleaseNode[]): string {
    const svg = this.requireSvg();
    const W = svg.clientWidth;
    const H = svg.clientHeight;

    const allCoordinates: { x: number; y: number }[] = nodes.map((n) => ({ x: n.position.x, y: n.position.y }));
    for (const link of this.allLinks()) {
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

    const contentHeightProportion = 0.65;
    const targetHeight = H * contentHeightProportion;
    const scale = targetHeight / Math.max(graphH, 1);
    const scaledGraphH = graphH * scale;
    const topPadding = (H - scaledGraphH) / 2;
    const translateY = -minY * scale + topPadding + ReleaseGraphComponent.RELEASE_GRAPH_NAVIGATION_PADDING;

    this.scale.set(scale);
    this.translateY.set(translateY);

    this.maxTranslateX = -minX * scale + W * 0.2;
    this.minTranslateX = W - maxX * scale - W * ReleaseGraphComponent.GRAPH_END_PADDING_PROPORTION;

    const latestReleaseTranslateX = W - latestReleaseX * scale - W * 0.35;
    this.translateX.set(this.clampTranslateX(latestReleaseTranslateX));

    const headerBottom = ReleaseGraphComponent.HEADER_HEIGHT_PX;
    const labelFontSize = ReleaseGraphComponent.QUARTER_LABEL_FONT_SIZE;
    const lineGap = ReleaseGraphComponent.QUARTER_LINE_GAP_PX;
    const lineOverflow = ReleaseGraphComponent.SVG_LINE_OVERFLOW_PX;

    this.svgLabelY.set((headerBottom - translateY) / scale);
    this.svgLineTopY.set((headerBottom + lineGap - translateY) / scale + labelFontSize);
    this.svgChevronY.set((headerBottom - translateY) / scale);
    this.svgLineBottomY.set((H + lineOverflow - translateY) / scale);

    return `0 0 ${W} ${H}`;
  }

  private collectLifecyclePhaseXPositions(): number[] {
    return this.branchLifecycles().flatMap((lifecycle) =>
      lifecycle.phases.flatMap((phase) => [phase.startX, phase.endX]),
    );
  }

  private lastQuarterMarkerX(): number {
    return this.quarterMarkers().at(-1)?.x ?? Number.NEGATIVE_INFINITY;
  }

  private calculateBranchLifecycles(releaseNodeMap: Map<string, ReleaseNode[]>): BranchLifecycle[] {
    if (!this.nodeService.timelineScale) return [];

    const lifecycles: BranchLifecycle[] = [];
    const nodesByY = this.groupNodesByYPosition([...releaseNodeMap.values()].flat());
    const sortedYPositions = [...nodesByY.keys()].toSorted((a, b) => a - b);

    for (const yPosition of sortedYPositions) {
      if (yPosition === 0) continue;

      const nodesAtY = nodesByY.get(yPosition)!;
      const branchLabel = this.determineBranchLabel(yPosition, nodesAtY, this.releases());

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
    return this.releaseNodes().find((n) => n.id === nodeId);
  }

  private findMajorMinorRelease(branchMajorMinor: string): Release | undefined {
    return this.releases().find((release) => {
      const releaseName = release.name.startsWith('v') ? release.name.slice(1) : release.name;
      return releaseName.startsWith(`${branchMajorMinor}.0`) && !releaseName.includes('nightly');
    });
  }

  private calculateSupportEndDate(branchStartDate: Date, totalSupportQuarters: number, isMajor: boolean): Date {
    const quartersPerExtendedWindow = isMajor ? 2 : 1;
    const extendedQuarters = quartersPerExtendedWindow * this.extendedSupportLevel();
    return this.addMonths(branchStartDate, (totalSupportQuarters + extendedQuarters) * 3);
  }

  private calculatePhaseBoundaries(
    branchStartDate: Date,
    offsetStartX: number,
    supportEndX: number,
    phaseMonths: number,
    scale: { startDate: Date; pixelsPerDay: number },
  ): { greenPhaseEndX: number; extendedPhaseEndsX: number[] } {
    const extendedSupportLevel = this.extendedSupportLevel();
    if (extendedSupportLevel === 0) {
      const midpointX = offsetStartX + (supportEndX - offsetStartX) / 2;
      return { greenPhaseEndX: midpointX, extendedPhaseEndsX: [] };
    }

    const greenPhaseEndX = this.calculateXPositionFromDate(this.addMonths(branchStartDate, phaseMonths), scale);

    const extendedPhaseEndsX = Array.from({ length: extendedSupportLevel }, (_, index) =>
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
    if (this.isLoading()) {
      this.isLoading.set(false);
      this.waitForSvgReady(() => {
        this.centerGraph();
        this.attachNonPassiveEventListeners();
      });
    }
  }

  private waitForSvgReady(callback: () => void): void {
    const svg = this.svgNative();

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
    const svg = this.svgNative();
    if (!svg) return;

    this.wheelListener = this.onWheel.bind(this);
    this.touchStartListener = this.onTouchStart.bind(this);
    this.touchMoveListener = this.onTouchMove.bind(this);

    svg.addEventListener('wheel', this.wheelListener, { passive: false });
    svg.addEventListener('touchstart', this.touchStartListener, { passive: false });
    svg.addEventListener('touchmove', this.touchMoveListener, { passive: false });
  }

  private removeNonPassiveEventListeners(): void {
    const svg = this.svgNative();
    if (!svg) return;

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
