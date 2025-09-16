import { Injectable } from '@angular/core';
import { Release } from '../../services/release.service';

export type Position = {
  x: number;
  y: number;
};

export type ReleaseNode = {
  id: string;
  label: string;
  position: Position;
  color: string;
  branch: string;
  originalBranch?: string;
  publishedAt: Date;
};

export const SupportColors = {
  FULL: '#30A102',
  SECURITY: '#EF9302',
  NONE: '#FD230E',
} as const;

@Injectable({ providedIn: 'root' })
export class ReleaseNodeService {
  private static readonly GITHUB_MASTER_BRANCH: string = 'master';
  private static readonly GITHUB_NIGHTLY_RELEASE: string = 'nightly';

  /**
   * Structureert de release-data voor de graaf.
   */
  public structureReleaseData(releases: Release[]): Map<string, ReleaseNode[]>[] {
    const hydratedReleases = releases.map((r) => ({
      ...r,
      publishedAt: new Date(r.publishedAt),
    }));

    const groupedByBranch = this.groupReleasesByBranch(hydratedReleases);
    this.sortGroupedReleases(groupedByBranch);

    const masterNodes = this.createReleaseNodes(groupedByBranch.get(ReleaseNodeService.GITHUB_MASTER_BRANCH) ?? []);
    groupedByBranch.delete(ReleaseNodeService.GITHUB_MASTER_BRANCH);

    const subBranchMaps: Map<string, ReleaseNode[]>[] = [];

    for (const [branchName, branchReleases] of groupedByBranch.entries()) {
      if (branchReleases.length === 0) continue;

      const subBranchNodes = this.createReleaseNodes(branchReleases);

      const anchorNode = subBranchNodes.shift()!;
      anchorNode.originalBranch = branchName;
      masterNodes.push(anchorNode);

      if (subBranchNodes.length > 0) {
        subBranchMaps.push(new Map([[branchName, subBranchNodes]]));
      }
    }

    this.sortByNightlyAndDate(masterNodes, (node) => node.label);

    const masterMap = new Map([[ReleaseNodeService.GITHUB_MASTER_BRANCH, masterNodes]]);
    return [masterMap, ...subBranchMaps];
  }

  public calculateReleaseCoordinates(structuredGroups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
    if (structuredGroups.length === 0) {
      return new Map();
    }

    const nodeMap = this.flattenGroupMaps(structuredGroups);
    const masterNodes = nodeMap.get(ReleaseNodeService.GITHUB_MASTER_BRANCH) ?? [];

    this.positionMasterNodes(masterNodes);

    const positionedNodes = new Map<string, ReleaseNode[]>();
    positionedNodes.set(ReleaseNodeService.GITHUB_MASTER_BRANCH, masterNodes);

    const subBranches = this.getSortedSubBranches(nodeMap);
    this.positionSubBranches(subBranches, masterNodes, positionedNodes);

    return positionedNodes;
  }

  public assignReleaseColors(releaseGroups: Map<string, ReleaseNode[]>): ReleaseNode[] {
    const allNodes: ReleaseNode[] = [];
    for (const nodes of releaseGroups.values()) {
      for (const node of nodes) {
        node.color = this.determineColor(node);
        allNodes.push(node);
      }
    }
    return allNodes;
  }

  private groupReleasesByBranch(
    releases: (Release & { publishedAt: Date })[],
  ): Map<string, (Release & { publishedAt: Date })[]> {
    const grouped = new Map<string, (Release & { publishedAt: Date })[]>();
    for (const release of releases) {
      const branch = release.branch.name;
      if (!grouped.has(branch)) {
        grouped.set(branch, []);
      }
      grouped.get(branch)!.push(release);
    }
    return grouped;
  }

  private sortGroupedReleases(grouped: Map<string, (Release & { publishedAt: Date })[]>): void {
    for (const releases of grouped.values()) {
      this.sortByNightlyAndDate(releases, (release) => release.name);
    }
  }

  private sortByNightlyAndDate<T extends { publishedAt: Date }>(nodes: T[], nameAccessor: (node: T) => string): void {
    nodes.sort((a, b) => {
      const aIsNightly = nameAccessor(a).toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE);
      const bIsNightly = nameAccessor(b).toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE);

      if (aIsNightly !== bIsNightly) {
        return aIsNightly ? 1 : -1;
      }

      return a.publishedAt.getTime() - b.publishedAt.getTime();
    });
  }

  private createReleaseNodes(releases: (Release & { publishedAt: Date })[]): ReleaseNode[] {
    return releases.map((r) => ({
      id: r.id,
      label: r.name,
      branch: r.branch.name,
      publishedAt: r.publishedAt,
      color: '',
      position: { x: 0, y: 0 },
    }));
  }

  private flattenGroupMaps(groupMaps: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
    const flatMap = new Map<string, ReleaseNode[]>();
    for (const group of groupMaps) {
      for (const [branch, nodes] of group.entries()) {
        flatMap.set(branch, nodes);
      }
    }
    return flatMap;
  }

  private positionMasterNodes(nodes: ReleaseNode[]): void {
    const SPACING = 250;
    for (const [index, node] of nodes.entries()) {
      node.position = { x: index * SPACING, y: 0 };
    }
  }

  private getSortedSubBranches(nodeMap: Map<string, ReleaseNode[]>): [string, ReleaseNode[]][] {
    return [...nodeMap.entries()]
      .filter(([branch]) => branch !== ReleaseNodeService.GITHUB_MASTER_BRANCH)
      .sort(([branchA], [branchB]) => {
        const versionA = this.getVersionFromBranchName(branchA);
        const versionB = this.getVersionFromBranchName(branchB);

        if (!versionA || !versionB) return 0;

        if (versionB.major !== versionA.major) {
          return versionB.major - versionA.major;
        }

        return versionB.minor - versionA.minor;
      });
  }

  private getVersionFromBranchName(branchName: string): { major: number; minor: number } | null {
    const match = branchName.match(/(\d+)\.(\d+)/);
    if (match && match[1] && match[2]) {
      return {
        major: Number.parseInt(match[1], 10),
        minor: Number.parseInt(match[2], 10),
      };
    }
    return null;
  }

  private positionSubBranches(
    subBranches: [string, ReleaseNode[]][],
    masterNodes: ReleaseNode[],
    positionedNodes: Map<string, ReleaseNode[]>,
  ): void {
    const Y_SPACING = 90;
    let yLevel = 1;

    for (const [branchName, subNodes] of subBranches) {
      if (subNodes.every((n) => this.isUnsupported(n))) continue;

      const anchorNode = masterNodes.find((n) => n.originalBranch === branchName);
      if (!anchorNode) continue;

      const baseX = anchorNode.position.x;
      const baseY = yLevel * Y_SPACING;

      this.positionSubBranchNodes(subNodes, baseX, baseY);

      positionedNodes.set(branchName, subNodes);
      yLevel++;
    }
  }

  private positionSubBranchNodes(subNodes: ReleaseNode[], baseX: number, baseY: number): void {
    const firstNode = subNodes[0];

    const BASE_X_SPACING = 102.5;
    const INITIAL_OFFSET = 100;

    const firstNodeLabelWidth = firstNode.label ? firstNode.label.length * 3 : 0;
    const labelBasedOffset = firstNodeLabelWidth / 2;

    const totalInitialOffset = INITIAL_OFFSET + labelBasedOffset;

    let currentX = baseX + totalInitialOffset;

    for (const node of subNodes) {
      node.position = { x: currentX, y: baseY };

      const labelWidth = node.label ? node.label.length * 8 : 0;
      currentX += BASE_X_SPACING + labelWidth / 2;
    }
  }

  private determineColor(release: ReleaseNode): string {
    if (release.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
      return 'darkblue';
    }

    const { fullSupportEnd, securitySupportEnd } = this.getSupportEndDates(release);
    const now = new Date();

    if (now <= fullSupportEnd) return SupportColors.FULL;
    if (now <= securitySupportEnd) return SupportColors.SECURITY;
    return SupportColors.NONE;
  }

  private isUnsupported(release: ReleaseNode): boolean {
    if (release.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
      return false;
    }
    const { securitySupportEnd } = this.getSupportEndDates(release);
    return new Date() > securitySupportEnd;
  }

  private getSupportEndDates(release: ReleaseNode): { fullSupportEnd: Date; securitySupportEnd: Date } {
    const versionMatch = release.label.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
    const isMajor = versionMatch
      ? Number.parseInt(versionMatch[3] ?? '0', 10) === 0 && !/rc|b/i.test(release.label)
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
}
