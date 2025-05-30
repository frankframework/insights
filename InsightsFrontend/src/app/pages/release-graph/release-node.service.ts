import { Injectable } from '@angular/core';
import { Release } from '../../services/release.service';
import { ReleaseGraphComponent } from './release-graph.component';

export interface Position {
  x: number;
  y: number;
}

export interface ReleaseNode {
  id: string;
  label: string;
  position: Position;
  color: string;
  branch: string;
  publishedAt: Date;
}

interface SyntheticNode extends ReleaseNode {
  isSynthetic?: boolean;
}

const SupportColors = {
  FULL: '#30A102',
  SECURITY: '#EF9302',
  NONE: '#FD230E',
} as const;

@Injectable({ providedIn: 'root' })
export class ReleaseNodeService {
  private static readonly GITHUB_NIGHTLY_RELEASE: string = 'nightly';

  public sortReleases(releases: Release[]): Map<string, ReleaseNode[]>[] {
    const grouped = this.groupReleasesByBranch(releases);
    this.sortGroupedReleases(grouped);

    const masterBranch = ReleaseGraphComponent.GITHUB_MASTER_BRANCH;
    const masterReleases = this.createReleaseNodes(grouped.get(masterBranch) ?? []);
    grouped.delete(masterBranch);

    const otherGroups = this.createOtherReleaseGroups(grouped, masterReleases, masterBranch);
    this.sortByDate(masterReleases);

    return [new Map([[masterBranch, masterReleases]]), ...otherGroups];
  }

  public calculateReleaseCoordinates(sortedGroups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
    const nodeMap = this.flattenGroupMaps(sortedGroups);

    const masterBranch = ReleaseGraphComponent.GITHUB_MASTER_BRANCH;
    const masterNodes = nodeMap.get(masterBranch) ?? [];
    this.positionMasterNodes(masterNodes);

    const positionedNodes = new Map<string, ReleaseNode[]>();
    positionedNodes.set(masterBranch, masterNodes);

    const subBranches = this.getSortedSubBranches(nodeMap, masterBranch);
    this.positionSubBranches(subBranches, masterNodes, positionedNodes);

    return positionedNodes;
  }

  public assignReleaseColors(releaseGroups: Map<string, ReleaseNode[]>): ReleaseNode[] {
    const allNodes: ReleaseNode[] = [];

    for (const nodes of releaseGroups.values()) {
      for (const release of nodes) {
        release.color = this.determineColor(release);
        allNodes.push(release);
      }
    }

    return allNodes;
  }

  private determineColor(release: ReleaseNode): string {
    const label = release.label.toLowerCase();
    if (label.includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
      return 'darkblue';
    }

    const { fullSupportEnd, securitySupportEnd } = this.getSupportEndDates(release);
    const now = new Date();

    if (now <= fullSupportEnd) return SupportColors.FULL;
    if (now <= securitySupportEnd) return SupportColors.SECURITY;
    return SupportColors.NONE;
  }

  private isUnsupported(release: ReleaseNode): boolean {
    const now = new Date();
    const { securitySupportEnd } = this.getSupportEndDates(release);
    return now > securitySupportEnd;
  }

  private getSupportEndDates(release: ReleaseNode): { fullSupportEnd: Date; securitySupportEnd: Date } {
    const versionMatch = release.label.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
    const isMajor = versionMatch
      ? Number.parseInt(versionMatch[3] ?? '0', 10) === 0 && !/rc|b/i.test(release.label.toLowerCase())
      : false;

    const fullSupportMonths = isMajor ? 12 : 3;
    const securitySupportMonths = isMajor ? 24 : 6;

    const published = new Date(release.publishedAt);

    const fullSupportEnd = new Date(published);
    fullSupportEnd.setMonth(published.getMonth() + fullSupportMonths);

    const securitySupportEnd = new Date(published);
    securitySupportEnd.setMonth(published.getMonth() + securitySupportMonths);

    return { fullSupportEnd, securitySupportEnd };
  }

  private groupReleasesByBranch(releases: Release[]): Map<string, Release[]> {
    const grouped = new Map<string, Release[]>();
    for (const release of releases) {
      const branch = release.branch.name;
      if (!grouped.has(branch)) grouped.set(branch, []);
      grouped.get(branch)!.push(release);
    }
    return grouped;
  }

  private sortGroupedReleases(grouped: Map<string, Release[]>): void {
    for (const [, releases] of grouped) {
      releases.sort((a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime());
    }
  }

  private createReleaseNodes(releases: Release[]): ReleaseNode[] {
    return releases.map((r) => ({
      id: r.id,
      label: r.name,
      branch: r.branch.name,
      publishedAt: new Date(r.publishedAt),
      color: '',
      position: { x: 0, y: 0 },
    }));
  }

  private createOtherReleaseGroups(
    grouped: Map<string, Release[]>,
    masterReleases: ReleaseNode[],
    masterBranch: string,
  ): Map<string, ReleaseNode[]>[] {
    return [...grouped.entries()].map(([branch, group]) => {
      const releaseNodes = this.createReleaseNodes(group);

      const syntheticNode = this.createSyntheticNodeIfNeeded(releaseNodes[0], branch, masterBranch);
      if (syntheticNode) {
        this.insertSyntheticNode(masterReleases, syntheticNode);
      }

      return new Map([[branch, releaseNodes]]);
    });
  }

  private createSyntheticNodeIfNeeded(
    firstNode: ReleaseNode,
    branch: string,
    masterBranch: string,
  ): SyntheticNode | null {
    const versionMatch = firstNode.label.match(/^v?(\d+)\.(\d+)/);
    if (!versionMatch) return null;

    return {
      id: `synthetic-${branch}-${firstNode.id}`,
      label: `v${versionMatch[1]}.${versionMatch[2]}`,
      position: { x: 0, y: 0 },
      color: 'gray',
      branch: masterBranch,
      publishedAt: new Date(firstNode.publishedAt),
      isSynthetic: true,
    };
  }

  private insertSyntheticNode(masterReleases: ReleaseNode[], syntheticNode: SyntheticNode): void {
    const insertIndex = masterReleases.findIndex((m) => m.publishedAt > syntheticNode.publishedAt);
    if (insertIndex === -1) {
      masterReleases.push(syntheticNode);
    } else {
      masterReleases.splice(insertIndex, 0, syntheticNode);
    }
  }

  private sortByDate(nodes: ReleaseNode[]): void {
    nodes.sort((a, b) => a.publishedAt.getTime() - b.publishedAt.getTime());
  }

  private flattenGroupMaps(groupMaps: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
    const result = new Map<string, ReleaseNode[]>();
    for (const group of groupMaps) {
      for (const [branch, nodes] of group.entries()) {
        result.set(branch, nodes);
      }
    }
    return result;
  }

  private positionMasterNodes(nodes: ReleaseNode[]): void {
    const spacing = 250;
    for (const [index, node] of nodes.entries()) {
      node.position = { x: index * spacing, y: 0 };
    }
  }

  private getSortedSubBranches(nodeMap: Map<string, ReleaseNode[]>, masterBranch: string): [string, ReleaseNode[]][] {
    return [...nodeMap.entries()]
      .filter(([branch]) => branch !== masterBranch)
      .sort(([, a], [, b]) => b[0].publishedAt.getTime() - a[0].publishedAt.getTime());
  }

  private positionSubBranches(
    subBranches: [string, ReleaseNode[]][],
    masterNodes: ReleaseNode[],
    positionedNodes: Map<string, ReleaseNode[]>,
  ): void {
    const baseXSpacing = 125;
    const ySpacing = 75;

    for (const [index, [branch, subNodes]] of subBranches.entries()) {
      if (subNodes.length === 0) continue;
      if (subNodes.every((n) => this.isUnsupported(n))) continue;

      const firstSub = subNodes[0];
      const versionMatch = firstSub.label.match(/^v?(\d+)\.(\d+)/);
      if (!versionMatch) continue;

      const synthetic = masterNodes.find(
        (n) => n.label === `v${versionMatch[1]}.${versionMatch[2]}` && (n as SyntheticNode).isSynthetic,
      );
      if (!synthetic) continue;

      const baseX = synthetic.position.x + 35;
      const baseY = (index + 1) * ySpacing;

      const positioned: ReleaseNode[] = [];
      let previousExtraSpacing = 0;

      for (const [index_, n] of subNodes.entries()) {
        const extraSpacing = n.label ? n.label.length * 5 : 0;
        const halfPrevious = previousExtraSpacing / 2;
        const halfThis = extraSpacing / 3;
        const x = baseX + index_ * baseXSpacing + halfPrevious + halfThis;
        const y = baseY;

        positioned.push({
          ...n,
          position: { x, y },
          extraSpacing,
        } as never);

        previousExtraSpacing = extraSpacing;
      }

      positionedNodes.set(branch, positioned);
    }
  }
}
