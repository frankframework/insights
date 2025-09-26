import { Injectable } from '@angular/core';
import { Release } from '../../services/release.service';

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
  originalBranch?: string;
  publishedAt: Date;
}

export const SupportColors = {
  FULL: '#30A102',
  SECURITY: '#EF9302',
  NONE: '#FD230E',
} as const;

// New interface to hold detailed version information
interface VersionInfo {
  major: number;
  minor: number;
  patch: number;
  type: 'major' | 'minor' | 'patch';
}

@Injectable({ providedIn: 'root' })
export class ReleaseNodeService {
  private static readonly GITHUB_MASTER_BRANCH: string = 'master';
  private static readonly GITHUB_NIGHTLY_RELEASE: string = 'nightly';

  public structureReleaseData(releases: Release[]): Map<string, ReleaseNode[]>[] {
    const hydratedReleases = releases.map((r) => ({
      ...r,
      publishedAt: new Date(r.publishedAt),
    }));

    const groupedByBranch = this.groupReleasesByBranch(hydratedReleases);
    this.sortGroupedReleases(groupedByBranch);

    // Prune unsupported minor branches BEFORE moving anchors to master
    this.pruneUnsupportedMinorBranches(groupedByBranch);

    const masterReleases = groupedByBranch.get(ReleaseNodeService.GITHUB_MASTER_BRANCH) ?? [];
    const masterNodes = this.createReleaseNodes(masterReleases);

    // Filter out unsupported minor releases from master branch as well
    const filteredMasterNodes = this.filterUnsupportedMinorReleases(masterNodes);

    groupedByBranch.delete(ReleaseNodeService.GITHUB_MASTER_BRANCH);

    const subBranchMaps: Map<string, ReleaseNode[]>[] = [];

    for (const [branchName, branchReleases] of groupedByBranch.entries()) {
      if (branchReleases.length === 0) continue;

      const subBranchNodes = this.createReleaseNodes(branchReleases);
      const anchorNode = subBranchNodes.shift()!;
      anchorNode.originalBranch = branchName;
      filteredMasterNodes.push(anchorNode);

      if (subBranchNodes.length > 0) {
        subBranchMaps.push(new Map([[branchName, subBranchNodes]]));
      }
    }

    this.sortByNightlyAndDate(filteredMasterNodes, (node) => node.label);

    const masterMap = new Map([[ReleaseNodeService.GITHUB_MASTER_BRANCH, filteredMasterNodes]]);
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

  /**
   * NEW: Removes minor release branches entirely if all their nodes are unsupported.
   */
  private pruneUnsupportedMinorBranches(groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>): void {
    for (const [branchName, releases] of groupedByBranch.entries()) {
      if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH || releases.length === 0) {
        continue;
      }

      const firstReleaseNode = this.createReleaseNodes([releases[0]])[0];
      const versionInfo = this.getVersionInfo(firstReleaseNode);

      // Only check minors; majors are preserved on the master line regardless
      if (versionInfo?.type === 'minor') {
        const allUnsupported = this.createReleaseNodes(releases).every((node) => this.isUnsupported(node));
        if (allUnsupported) {
          groupedByBranch.delete(branchName);
        }
      }
    }
  }

  /**
   * NEW: Filters out unsupported minor releases from the master branch that don't have patch versions.
   * These are versions like v6.1, v7.4 that should be hidden if they don't have supported patches.
   */
  private filterUnsupportedMinorReleases(masterNodes: ReleaseNode[]): ReleaseNode[] {
    return masterNodes.filter((node) => {
      const versionInfo = this.getVersionInfo(node);

      // Keep nightly releases
      if (node.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
        return true;
      }

      // Keep major releases (like v6.0, v7.0)
      if (versionInfo?.type === 'major') {
        return true;
      }

      // For minor releases (like v6.1, v7.4), check if they have support
      if (versionInfo?.type === 'minor') {
        return !this.isUnsupported(node);
      }

      // Keep patch releases
      return true;
    });
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
      if (aIsNightly !== bIsNightly) return aIsNightly ? 1 : -1;
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
    const BASE_SPACING = 250;
    let currentX = 0;

    // Check if there's a gap before the first node
    if (nodes.length > 0) {
      const firstNode = nodes[0];
      const hasInitialGap = this.hasInitialVersionGap(firstNode);
      if (hasInitialGap) {
        currentX = BASE_SPACING * 1.8;
      }
    }

    nodes.forEach((node, index) => {
      node.position = { x: currentX, y: 0 };

      if (index < nodes.length - 1) {
        const nextNode = nodes[index + 1];
        const hasVersionGap = this.isVersionGap(node, nextNode);
        const spacing = hasVersionGap ? BASE_SPACING * 1.8 : BASE_SPACING;
        currentX += spacing;
      }
    });
  }

  private hasInitialVersionGap(firstNode: ReleaseNode): boolean {
    const vFirst = this.getVersionInfo(firstNode);
    if (!vFirst) return false;
    return vFirst.major > 1 || (vFirst.major === 1 && vFirst.minor > 0);
  }

  private isVersionGap(source: ReleaseNode, target: ReleaseNode): boolean {
    const vSource = this.getVersionInfo(source);
    const vTarget = this.getVersionInfo(target);
    if (!vSource || !vTarget) return false;
    const majorGap = vTarget.major > vSource.major + 1;
    const minorGap = vSource.major === vTarget.major && vTarget.minor > vSource.minor + 1;
    return majorGap || minorGap;
  }

  private getSortedSubBranches(nodeMap: Map<string, ReleaseNode[]>): [string, ReleaseNode[]][] {
    return [...nodeMap.entries()]
      .filter(([branch]) => branch !== ReleaseNodeService.GITHUB_MASTER_BRANCH)
      .sort(([branchA], [branchB]) => {
        const versionA = this.getVersionFromBranchName(branchA);
        const versionB = this.getVersionFromBranchName(branchB);
        if (!versionA || !versionB) return 0;
        if (versionB.major !== versionA.major) return versionB.major - versionA.major;
        return versionB.minor - versionA.minor;
      });
  }

  private getVersionFromBranchName(branchName: string): { major: number; minor: number } | null {
    const match = branchName.match(/(\d+)\.(\d+)/);
    return match && match[1] && match[2] ? { major: parseInt(match[1], 10), minor: parseInt(match[2], 10) } : null;
  }

  private positionSubBranches(
    subBranches: [string, ReleaseNode[]][],
    masterNodes: ReleaseNode[],
    positionedNodes: Map<string, ReleaseNode[]>,
  ): void {
    const Y_SPACING = 90;
    let yLevel = 1;

    for (const [branchName, subNodes] of subBranches) {
      // MODIFIED: Hide sub-branch if all its nodes are unsupported (applies to major patches)
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
    if (release.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) return 'darkblue';

    const supportDates = this.getSupportEndDates(release);
    if (!supportDates) return SupportColors.NONE;

    const { fullSupportEnd, securitySupportEnd } = supportDates;
    const now = new Date();

    if (now <= fullSupportEnd) return SupportColors.FULL;
    if (now <= securitySupportEnd) return SupportColors.SECURITY;
    return SupportColors.NONE;
  }

  private isUnsupported(release: ReleaseNode): boolean {
    if (release.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) return false;

    const supportDates = this.getSupportEndDates(release);
    if (!supportDates) return true;

    return new Date() > supportDates.securitySupportEnd;
  }

  /**
   * REWRITTEN: Now correctly identifies version type for accurate support duration.
   */
  private getSupportEndDates(release: ReleaseNode): { fullSupportEnd: Date; securitySupportEnd: Date } | null {
    const versionInfo = this.getVersionInfo(release);
    if (!versionInfo) return null;

    const fullSupportMonths = versionInfo.type === 'major' ? 6 : 3;
    const securitySupportMonths = versionInfo.type === 'major' ? 12 : 6;

    const published = new Date(release.publishedAt);
    const fullSupportEnd = new Date(published);
    fullSupportEnd.setMonth(published.getMonth() + fullSupportMonths);
    const securitySupportEnd = new Date(published);
    securitySupportEnd.setMonth(published.getMonth() + securitySupportMonths);

    return { fullSupportEnd, securitySupportEnd };
  }

  /**
   * NEW: Robustly parses a version string to determine its type (major, minor, patch).
   */
  public getVersionInfo(release: ReleaseNode): VersionInfo | null {
    const match = release.label.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
    if (!match) return null;

    const major = parseInt(match[1], 10);
    const minor = parseInt(match[2], 10);
    const patch = parseInt(match[3] ?? '0', 10);

    let type: VersionInfo['type'] = 'patch';
    if (patch === 0 && minor > 0) {
      type = 'minor';
    } else if (patch === 0 && minor === 0) {
      type = 'major';
    }

    return { major, minor, patch, type };
  }
}
