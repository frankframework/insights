import { Component, ElementRef, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { Release, ReleaseService } from '../../services/release.service';
import { catchError, map, of, tap } from 'rxjs';
import { ReleaseNode, ReleaseNodeService, QuarterMarker } from './release-node.service';
import { ReleaseLink, ReleaseLinkService, SkipNode } from './release-link.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ReleaseCatalogusComponent } from './release-catalogus/release-catalogus.component';
import { ReleaseSkippedVersions } from './release-skipped-versions/release-skipped-versions';
import { KeyValuePipe } from '@angular/common';

export interface LifecyclePhase {
  type: 'supported';
  startX: number;
  endX: number;
  color: string;
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
  imports: [LoaderComponent, ReleaseCatalogusComponent, ReleaseSkippedVersions, KeyValuePipe],
})
export class ReleaseGraphComponent implements OnInit, OnDestroy {
  private static readonly RELEASE_GRAPH_NAVIGATION_PADDING: number = 75;
  private static readonly SKIP_RELEASE_NODE_BEGIN: string = 'skip-initial-';

  @ViewChild('svgElement') svgElement!: ElementRef<SVGSVGElement>;

  public releaseNodes: ReleaseNode[] = [];
  public allLinks: ReleaseLink[] = [];
  public branchLabels: { label: string; y: number; x: number; supportStatus?: 'supported' | 'none' }[] = [];
  public stickyBranchLabels: { label: string; screenY: number; supportStatus?: 'supported' | 'none' }[] = [];
  public skipNodes: SkipNode[] = [];
  public dataForSkipModal: SkipNode | null = null;
  public quarterMarkers: QuarterMarker[] = [];
  public expandedClusters = new Map<string, ReleaseNode>();
  public branchLifecycles: BranchLifecycle[] = [];
  public currentTimeX = 0;

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
  private toastService = inject(ToastrService);

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

    const nodeRadius = 20;

    const margin = isSkipLink ? 10 : nodeRadius + 2;

    if (link.isGap || link.isFadeIn) {
      const [x1, y1] = [source.position.x, source.position.y];
      const [x2, y2] = [target.position.x, target.position.y];
      const startMargin = link.isFadeIn ? 0 : margin;
      return `M ${x1 + startMargin},${y1} L ${x2 - margin},${y2}`;
    }

    const [x1, y1] = [source.position.x, source.position.y];
    const [x2, y2] = [target.position.x, target.position.y];

    if (y1 === y2) {
      return `M ${x1 + margin},${y1} L ${x2 - margin},${y2}`;
    }

    const verticalDirection = y2 > y1 ? 1 : -1;
    const cornerY = y2 - verticalDirection * margin;
    const horizontalSweep = x2 > x1 ? 0 : 1;

    return [
      `M ${x1},${y1 + margin}`,
      `L ${x1},${cornerY}`,
      `A ${margin},${margin} 0 0,${horizontalSweep} ${x1 + (horizontalSweep ? -margin : margin)},${y2}`,
      `L ${x2 - margin},${y2}`,
    ].join(' ');
  }

  public openReleaseNodeDetails(releaseNodeId: string): void {
    this.router.navigate(['/graph', releaseNodeId]);
  }

  public toggleCluster(clusterNode: ReleaseNode): void {
    if (!clusterNode.isCluster) return;

    const nodeIndex = this.releaseNodes.findIndex((n) => n.id === clusterNode.id);
    if (nodeIndex === -1) return;

    if (clusterNode.isExpanded) {
      clusterNode.isExpanded = false;
      const expandedCount = clusterNode.clusteredNodes?.length ?? 0;
      this.releaseNodes.splice(nodeIndex, expandedCount, clusterNode);
      this.expandedClusters.delete(clusterNode.id);
    } else {
      clusterNode.isExpanded = true;
      const expandedNodes = this.nodeService.expandCluster(clusterNode);
      this.releaseNodes.splice(nodeIndex, 1, ...expandedNodes);
      this.expandedClusters.set(clusterNode.id, clusterNode);
    }
  }

  public collapseCluster(clusterId: string): void {
    const clusterNode = this.expandedClusters.get(clusterId);
    if (!clusterNode) return;

    const firstExpandedIndex = this.releaseNodes.findIndex((n) =>
      clusterNode.clusteredNodes?.some((cn) => cn.id === n.id),
    );

    if (firstExpandedIndex !== -1) {
      const expandedCount = clusterNode.clusteredNodes?.length ?? 0;
      clusterNode.isExpanded = false;
      this.releaseNodes.splice(firstExpandedIndex, expandedCount, clusterNode);
      this.expandedClusters.delete(clusterId);
    }
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

  public getLastExpandedNodePosition(clusterNode: ReleaseNode): { x: number; y: number } | null {
    if (!clusterNode.isExpanded || !clusterNode.clusteredNodes || clusterNode.clusteredNodes.length === 0) {
      return null;
    }

    const lastNode = clusterNode.clusteredNodes.at(-1);
    if (!lastNode) {
      return null;
    }
    const lastNodeInArray = this.releaseNodes.find((n) => n.id === lastNode.id);
    return lastNodeInArray ? lastNodeInArray.position : null;
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

    const clusterNode = this.releaseNodes.find((n) => n.isCluster && n.clusteredNodes?.some((cn) => cn.id === id));
    if (clusterNode) {
      return clusterNode;
    }

    return undefined;
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
          this.allLinks = [];
          this.toastService.error('Failed to load releases. Please try again later.');
          this.checkReleaseGraphLoading();
          return of([]);
        }),
      )
      .subscribe();
  }

  private buildReleaseGraph(sortedGroups: Map<string, ReleaseNode[]>[]): void {
    const releaseNodeMap = this.nodeService.calculateReleaseCoordinates(sortedGroups);
    this.nodeService.assignReleaseColors(releaseNodeMap);

    const clusteredNodeMap = new Map<string, ReleaseNode[]>();
    for (const [branch, nodes] of releaseNodeMap.entries()) {
      clusteredNodeMap.set(branch, this.nodeService.createClusters(nodes));
    }

    this.releaseNodes = [];
    for (const nodes of clusteredNodeMap.values()) {
      this.releaseNodes.push(...nodes);
    }

    this.skipNodes = this.linkService.createSkipNodes(sortedGroups, this.releases);

    this.updateSkipNodesForClusters();

    const masterNodes = clusteredNodeMap.get('master') ?? [];
    const skipNodeLinks = this.linkService.createSkipNodeLinks(this.skipNodes, masterNodes);

    this.allLinks = [...this.linkService.createLinks(sortedGroups, this.skipNodes), ...skipNodeLinks];
    this.branchLabels = this.createBranchLabels(releaseNodeMap, this.releases);
    this.branchLifecycles = this.calculateBranchLifecycles(releaseNodeMap);

    this.quarterMarkers = this.nodeService.timelineScale?.quarters ?? [];

    // Calculate current time X position
    if (this.nodeService.timelineScale) {
      this.currentTimeX = this.calculateXPositionFromDate(new Date(), this.nodeService.timelineScale);
    }

    this.checkReleaseGraphLoading();
  }

  private updateSkipNodesForClusters(): void {
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
  ): { label: string; y: number; x: number; supportStatus?: 'supported' | 'none' }[] {
    const labels: { label: string; y: number; x: number; supportStatus?: 'supported' | 'none' }[] = [];
    const allNodes = [...releaseNodeMap.values()].flat();
    const labelX = Math.min(...allNodes.map((n) => n.position.x)) - 550;
    const nodesByY = this.groupNodesByYPosition(allNodes);
    const sortedYPositions = [...nodesByY.keys()].sort((a, b) => a - b);

    for (const yPosition of sortedYPositions) {
      const branchLabel = this.determineBranchLabel(yPosition, nodesByY.get(yPosition)!, releases);
      // Master branch (y === 0) is always supported
      const supportStatus = yPosition === 0 ? 'supported' : this.getBranchSupportStatus(nodesByY.get(yPosition)!);
      this.addUniqueBranchLabel(labels, branchLabel, yPosition, labelX, supportStatus);
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
    labels: { label: string; y: number; x: number; supportStatus?: 'supported' | 'none' }[],
    branchLabel: string,
    yPosition: number,
    labelX: number,
    supportStatus?: 'supported' | 'none',
  ): void {
    const existingLabel = labels.find((l) => l.y === yPosition);
    if (!existingLabel) {
      labels.push({
        label: branchLabel,
        y: yPosition,
        x: labelX,
        supportStatus,
      });
    }
  }

  private getBranchSupportStatus(nodesAtY: ReleaseNode[]): 'supported' | 'none' {
    // Get all nodes including those in clusters
    const allNodes = this.getAllNodesInBranch(nodesAtY);
    if (allNodes.length === 0) return 'none';

    // Get the first node to determine branch version (e.g., v9.2.1 -> 9.2)
    const sortedNodes = [...allNodes].sort((a, b) => a.position.x - b.position.x);
    const firstNode = sortedNodes[0];
    const firstVersionInfo = this.nodeService.getVersionInfo(firstNode);
    if (!firstVersionInfo) return 'none';

    // Find the corresponding major/minor release from all releases
    const branchMajorMinor = `${firstVersionInfo.major}.${firstVersionInfo.minor}`;
    const majorMinorRelease = this.releases.find((release) => {
      const releaseName = release.name.startsWith('v') ? release.name.slice(1) : release.name;
      return releaseName.startsWith(`${branchMajorMinor}.0`) && !releaseName.includes('nightly');
    });

    if (!majorMinorRelease) return 'none';

    const majorMinorNode = this.releaseNodes.find((node) => node.id === majorMinorRelease.id);
    if (!majorMinorNode) return 'none';

    const versionInfo = this.nodeService.getVersionInfo(majorMinorNode);
    if (!versionInfo) return 'none';

    // Minor: 2 quarters (6 months)
    // Major: 4 quarters (12 months)
    const totalSupportQuarters = versionInfo.type === 'major' ? 4 : 2;

    const branchStartDate = new Date(majorMinorRelease.publishedAt);
    const supportEnd = new Date(branchStartDate);
    // Add quarters (each quarter is 3 months)
    supportEnd.setMonth(branchStartDate.getMonth() + totalSupportQuarters * 3);

    const now = new Date();

    if (now <= supportEnd) return 'supported';
    return 'none';
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
        supportStatus: label.supportStatus,
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

  private calculateBranchLifecycles(releaseNodeMap: Map<string, ReleaseNode[]>): BranchLifecycle[] {
    if (!this.nodeService.timelineScale) return [];

    const lifecycles: BranchLifecycle[] = [];
    const nodesByY = this.groupNodesByYPosition([...releaseNodeMap.values()].flat());
    const sortedYPositions = [...nodesByY.keys()].sort((a, b) => a - b);

    for (const yPosition of sortedYPositions) {
      // Skip master branch (y === 0)
      if (yPosition === 0) continue;

      const nodesAtY = nodesByY.get(yPosition)!;
      const branchLabel = this.determineBranchLabel(yPosition, nodesAtY, this.releases);

      // Get all nodes for this branch (including those in clusters)
      const allNodesInBranch = this.getAllNodesInBranch(nodesAtY);

      // Sort nodes by x position
      const sortedNodes = [...allNodesInBranch].sort((a, b) => a.position.x - b.position.x);

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
    const allNodes: ReleaseNode[] = [];

    for (const node of nodes) {
      if (node.isCluster && node.clusteredNodes) {
        allNodes.push(...node.clusteredNodes);
      } else {
        allNodes.push(node);
      }
    }

    return allNodes;
  }

  private calculateLifecyclePhasesForBranch(sortedNodes: ReleaseNode[]): LifecyclePhase[] {
    if (!this.nodeService.timelineScale || sortedNodes.length === 0) return [];

    const phases: LifecyclePhase[] = [];
    const scale = this.nodeService.timelineScale;

    // Get the first node to determine branch version (e.g., v9.2.1 -> 9.2)
    const firstNode = sortedNodes[0];
    const firstVersionInfo = this.nodeService.getVersionInfo(firstNode);
    if (!firstVersionInfo) return [];

    // Find the corresponding major/minor release from all releases
    // For example, if branch has v9.2.1, find v9.2.0 from the releases
    const branchMajorMinor = `${firstVersionInfo.major}.${firstVersionInfo.minor}`;
    const majorMinorRelease = this.releases.find((release) => {
      const releaseName = release.name.startsWith('v') ? release.name.slice(1) : release.name;
      return releaseName.startsWith(`${branchMajorMinor}.0`) && !releaseName.includes('nightly');
    });

    if (!majorMinorRelease) return [];

    // Find the corresponding node in releaseNodes to get the X position
    const majorMinorNode = this.releaseNodes.find((node) => node.id === majorMinorRelease.id);
    if (!majorMinorNode) return [];

    const firstX = majorMinorNode.position.x;
    const versionInfo = this.nodeService.getVersionInfo(majorMinorNode);
    if (!versionInfo) return [];

    // Minor: 2 quarters (6 months)
    // Major: 4 quarters (12 months)
    const totalSupportQuarters = versionInfo.type === 'major' ? 4 : 2;

    const branchStartDate = new Date(majorMinorRelease.publishedAt);
    const supportEnd = new Date(branchStartDate);
    // Add quarters (each quarter is 3 months)
    supportEnd.setMonth(branchStartDate.getMonth() + totalSupportQuarters * 3);

    // Calculate X positions for lifecycle boundaries
    const supportEndX = this.calculateXPositionFromDate(supportEnd, scale);

    // Create single support phase with light green color
    phases.push({
      type: 'supported',
      startX: firstX,
      endX: supportEndX,
      color: 'rgba(144, 238, 144, 0.2)', // Light green with transparency
    });

    return phases;
  }

  private calculateXPositionFromDate(date: Date, scale: { startDate: Date; pixelsPerDay: number }): number {
    const daysSinceStart = (date.getTime() - scale.startDate.getTime()) / (1000 * 60 * 60 * 24);
    return daysSinceStart * scale.pixelsPerDay;
  }

  private checkReleaseGraphLoading(): void {
    if (this.isLoading) {
      this.isLoading = false;
      setTimeout(() => this.centerGraph(), 50);
    }
  }
}
