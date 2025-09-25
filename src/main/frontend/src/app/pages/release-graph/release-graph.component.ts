import { Component, ElementRef, OnInit, OnDestroy, ViewChild, inject } from '@angular/core';
import { Release, ReleaseService } from '../../services/release.service';
import { BehaviorSubject, catchError, map, of, tap } from 'rxjs';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { ReleaseLink, ReleaseLinkService, SkipNode } from './release-link.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { ReleaseOffCanvasComponent } from './release-off-canvas/release-off-canvas.component';
import { AsyncPipe } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ReleaseCatalogusComponent } from './release-catalogus/release-catalogus.component';

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
  // MODIFIED: One single array for all links
  public allLinks: ReleaseLink[] = [];
  public branchLabels: { label: string; y: number; x: number }[] = [];
  public stickyBranchLabels: { label: string; screenY: number }[] = [];
  public skipNodes: SkipNode[] = [];

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

  public onWheel(event: WheelEvent): void {
    event.preventDefault();
    const delta = (event.deltaX === 0 ? event.deltaY : event.deltaX) / this.scale;
    const newTranslateX = this.translateX - delta;
    this.translateX = Math.min(this.maxTranslateX, Math.max(this.minTranslateX, newTranslateX));
    this.updateStickyBranchLabels();
  }

  private findNodeById(id: string): ReleaseNode | undefined {
    if (id.startsWith('start-node-') && this.releaseNodes.length > 0) {
      const firstNode = this.releaseNodes.find((n) => n.position.x === 0);
      if (firstNode) {
        return {
          ...firstNode,
          id: id,
          position: { x: firstNode.position.x - 350, y: firstNode.position.y }, // Increased from 250 to 400 for longer fade-in line
        };
      }
    }

    // Check if it's a skip node
    const skipNode = this.skipNodes.find(s => s.id === id);
    if (skipNode) {
      return {
        id: skipNode.id,
        label: skipNode.label,
        position: { x: skipNode.x, y: skipNode.y },
        color: '#ccc', // Skip nodes have gray color
        branch: 'skip',
        publishedAt: new Date()
      };
    }

    return this.releaseNodes.find((n) => n.id === id);
  }

  public getCustomPath(link: ReleaseLink): string {
    const source = this.findNodeById(link.source);
    const target = this.findNodeById(link.target);
    if (!source || !target) return '';

    if (link.isGap || link.isFadeIn) {
      const [x1, y1] = [source.position.x, source.position.y];
      const [x2, y2] = [target.position.x, target.position.y];
      const releaseNodeRadiusWithMargin = link.isFadeIn ? 0 : 25;
      return `M ${x1 + releaseNodeRadiusWithMargin},${y1} L ${x2 - releaseNodeRadiusWithMargin},${y2}`;
    }

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

  public openSkipNodeDetails(skipNode: SkipNode): void {
    // Find the first actual release that matches one of the skipped versions
    const skippedRelease = this.releases.find(release =>
      skipNode.skippedVersions.some(version =>
        release.name === version || release.name === version.replace('v', '')
      )
    );

    if (skippedRelease) {
      this._selectedRelease.next(skippedRelease);
    } else {
      // If no matching release found, find the first release that matches the pattern
      const firstSkippedVersion = skipNode.skippedVersions[0];
      const matchingRelease = this.releases.find(release => {
        const releaseVersion = release.name.replace(/^v/, '');
        const skippedVersion = firstSkippedVersion.replace(/^v/, '');
        return releaseVersion.startsWith(skippedVersion);
      });

      if (matchingRelease) {
        this._selectedRelease.next(matchingRelease);
      }
    }
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
    this.releaseNodes = this.nodeService.assignReleaseColors(releaseNodeMap);

    // Create skip nodes and their links
    this.skipNodes = this.linkService.createSkipNodes(sortedGroups);
    const masterNodes = releaseNodeMap.get('master') ?? [];
    const skipNodeLinks = this.linkService.createSkipNodeLinks(this.skipNodes, masterNodes);

    // Combine regular links with skip node links
    this.allLinks = [...this.linkService.createLinks(sortedGroups), ...skipNodeLinks];
    this.branchLabels = this.createBranchLabels(releaseNodeMap, this.releases);

    this.checkReleaseGraphLoading();
  }

  private createBranchLabels(releaseNodeMap: Map<string, ReleaseNode[]>, releases: Release[]): { label: string; y: number; x: number }[] {
    const labels: { label: string; y: number; x: number }[] = [];

    const allNodes = [...releaseNodeMap.values()].flat();
    const minX = Math.min(...allNodes.map(n => n.position.x));
    const labelX = minX - 550; // Position labels further to the left of the fade-in line

    // Group nodes by Y position to find the most common branch at each level
    const nodesByY = new Map<number, ReleaseNode[]>();
    for (const node of allNodes) {
      if (!nodesByY.has(node.position.y)) {
        nodesByY.set(node.position.y, []);
      }
      nodesByY.get(node.position.y)!.push(node);
    }

    // Sort Y positions to ensure consistent ordering
    const sortedYPositions = Array.from(nodesByY.keys()).sort((a, b) => a - b);

    // For each Y level, find the appropriate branch name
    for (const yPosition of sortedYPositions) {
      const nodesAtY = nodesByY.get(yPosition)!;
      let branchLabel = 'unknown';

      if (yPosition === 0) {
        // For the top level (master line), look for actual master nodes
        const masterNodes = nodesAtY.filter(node => !node.originalBranch);
        if (masterNodes.length > 0) {
          const masterRelease = releases.find(r => r.id === masterNodes[0].id);
          branchLabel = masterRelease?.branch?.name || 'master';
        } else {
          branchLabel = 'master';
        }
      } else {
        // For sub-branches, find the most common branch name
        const branchCounts = new Map<string, number>();

        for (const node of nodesAtY) {
          const release = releases.find(r => r.id === node.id);
          const branchName = release?.branch?.name || 'unknown';
          branchCounts.set(branchName, (branchCounts.get(branchName) || 0) + 1);
        }

        let maxCount = 0;
        for (const [branchName, count] of branchCounts.entries()) {
          if (count > maxCount) {
            maxCount = count;
            branchLabel = branchName;
          }
        }
      }

      // Only add unique branch labels to avoid duplicates
      const existingLabel = labels.find(l => l.y === yPosition);
      if (!existingLabel) {
        labels.push({
          label: branchLabel,
          y: yPosition,
          x: labelX
        });
      }
    }

    return labels;
  }

  private centerGraph(): void {
    if (!this.svgElement?.nativeElement || this.releaseNodes.length === 0) return;
    this.viewBox = this.calculateViewBox(this.releaseNodes);
    this.updateStickyBranchLabels();
  }

  private updateStickyBranchLabels(): void {
    if (!this.svgElement?.nativeElement) return;

    this.stickyBranchLabels = this.branchLabels.map(label => {
      // Convert SVG coordinates to screen coordinates
      const svgY = label.y * this.scale + this.translateY;
      const svg = this.svgElement.nativeElement;
      const svgRect = svg.getBoundingClientRect();
      const screenY = svgRect.top + svgY;

      return {
        label: label.label,
        screenY: screenY
      };
    });
  }

  private calculateViewBox(nodes: ReleaseNode[]): string {
    const svg = this.svgElement.nativeElement;
    const W = svg.clientWidth;
    const H = svg.clientHeight;

    // Calculate bounds based on link coordinates (including fade-in line)
    const allCoordinates: { x: number; y: number }[] = [];

    // Add all node coordinates
    allCoordinates.push(...nodes.map(n => ({ x: n.position.x, y: n.position.y })));

    // Add link endpoint coordinates (including virtual start nodes)
    for (const link of this.allLinks) {
      const source = this.findNodeById(link.source);
      const target = this.findNodeById(link.target);
      if (source && target) {
        allCoordinates.push({ x: source.position.x, y: source.position.y });
        allCoordinates.push({ x: target.position.x, y: target.position.y });
      }
    }

    const xs = allCoordinates.map(coord => coord.x);
    const ys = allCoordinates.map(coord => coord.y);
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
