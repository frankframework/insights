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
  isCluster?: boolean;
  clusteredNodes?: ReleaseNode[];
  isExpanded?: boolean;
  isMiniNode?: boolean;
  linkedBranchNode?: string;
}

export interface TimelineScale {
  startDate: Date;
  endDate: Date;
  pixelsPerDay: number;
  totalDays: number;
  quarters: QuarterMarker[];
  latestReleaseDate: Date;
}

export interface QuarterMarker {
  label: string;
  date: Date;
  x: number;
  labelX: number;
  year: number;
  quarter: number;
}

export const SupportColors = {
  FULL: '#30A102',
  SECURITY: '#EF9302',
  NONE: '#FD230E',
  NIGHTLY: 'darkblue',
} as const;

export type SupportColor = (typeof SupportColors)[keyof typeof SupportColors];

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
  private static readonly GITHUB_SNAPSHOT_DISPLAY: string = 'snapshot';

  public timelineScale: TimelineScale | null = null;

  public structureReleaseData(releases: Release[]): Map<string, ReleaseNode[]>[] {
    const hydratedReleases = releases.map((r) => ({
      ...r,
      publishedAt: new Date(r.publishedAt),
    }));

    const groupedByBranch = this.groupReleasesByBranch(hydratedReleases);
    this.sortGroupedReleases(groupedByBranch);

    this.removeDuplicateNightlies(groupedByBranch);

    this.pruneUnsupportedMinorBranches(groupedByBranch);
    this.filterLowVersionNightliesFromBranches(groupedByBranch);

    const masterReleases = groupedByBranch.get(ReleaseNodeService.GITHUB_MASTER_BRANCH) ?? [];
    const nodes = this.createReleaseNodes(masterReleases);

    const filteredNodes = this.filterUnsupportedMinorReleases(nodes);

    groupedByBranch.delete(ReleaseNodeService.GITHUB_MASTER_BRANCH);

    const branchMaps: Map<string, ReleaseNode[]>[] = [];

    for (const [branchName, branchReleases] of groupedByBranch.entries()) {
      if (branchReleases.length === 0) continue;

      const branchNodes = this.createReleaseNodes(branchReleases);
      const firstBranchNode = branchNodes[0];

      const miniNode: ReleaseNode = {
        id: `mini-${firstBranchNode.id}`,
        label: '',
        branch: ReleaseNodeService.GITHUB_MASTER_BRANCH,
        publishedAt: firstBranchNode.publishedAt,
        color: '',
        position: { x: 0, y: 0 },
        isMiniNode: true,
        originalBranch: branchName,
        linkedBranchNode: firstBranchNode.id,
      };

      filteredNodes.push(miniNode);
      branchMaps.push(new Map([[branchName, branchNodes]]));
    }

    this.sortByNightlyAndDate(filteredNodes, (node) => node.label);

    const masterMap = new Map([[ReleaseNodeService.GITHUB_MASTER_BRANCH, filteredNodes]]);
    return [masterMap, ...branchMaps];
  }

  public calculateReleaseCoordinates(structuredGroups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
    if (structuredGroups.length === 0) {
      return new Map();
    }

    const nodeMap = this.flattenGroupMaps(structuredGroups);

    // Get all nodes to calculate timeline scale
    const allNodes: ReleaseNode[] = [];
    for (const nodes of nodeMap.values()) {
      allNodes.push(...nodes);
    }

    // Calculate timeline scale based on all publish dates
    this.timelineScale = this.calculateTimelineScale(allNodes);

    const masterBranchNodes = nodeMap.get(ReleaseNodeService.GITHUB_MASTER_BRANCH) ?? [];
    this.positionMasterNodes(masterBranchNodes);

    const positionedNodes = new Map<string, ReleaseNode[]>([
      [ReleaseNodeService.GITHUB_MASTER_BRANCH, masterBranchNodes],
    ]);

    const branches = this.getSortedBranches(nodeMap);
    this.positionBranches(branches, masterBranchNodes, positionedNodes);

    return positionedNodes;
  }

  public assignReleaseColors(releaseGroups: Map<string, ReleaseNode[]>): void {
    const allNodes: ReleaseNode[] = [];
    for (const nodes of releaseGroups.values()) {
      allNodes.push(...nodes.filter((n) => !n.isMiniNode));
    }

    const parentVersionMap = this.buildParentVersionMap(allNodes);
    const versionGroups = new Map<string, ReleaseNode[]>();
    for (const node of allNodes) {
      const versionInfo = this.getVersionInfo(node);
      if (versionInfo && versionInfo.patch > 0) {
        const key = `${versionInfo.major}.${versionInfo.minor}`;
        if (!versionGroups.has(key)) {
          versionGroups.set(key, []);
        }
        versionGroups.get(key)!.push(node);
      }
    }

    const latestPatches = new Set<string>();
    for (const nodesInGroup of versionGroups.values()) {
      const sortedByPatch = [...nodesInGroup].toSorted((a, b) => {
        const vA = this.getVersionInfo(a)!;
        const vB = this.getVersionInfo(b)!;
        return vB.patch - vA.patch;
      });

      if (sortedByPatch.length > 0) {
        latestPatches.add(sortedByPatch[0].id);
      }
    }

    for (const node of allNodes) {
      const versionInfo = this.getVersionInfo(node);
      const isPatchVersion = versionInfo !== null && versionInfo.patch > 0;
      const isLatestPatch = latestPatches.has(node.id);
      const parentNode = isPatchVersion
        ? (parentVersionMap.get(`${versionInfo!.major}.${versionInfo!.minor}`) ?? null)
        : null;
      node.color = this.determineColor(node, isPatchVersion, isLatestPatch, parentNode);
    }
  }

  public getVersionInfo(release: ReleaseNode): VersionInfo | null {
    const match = release.label.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
    if (!match) return null;

    const major = Number.parseInt(match[1], 10);
    const minor = Number.parseInt(match[2], 10);
    const patch = Number.parseInt(match[3] ?? '0', 10);

    let type: VersionInfo['type'] = 'patch';
    if (patch === 0 && minor > 0) {
      type = 'minor';
    } else if (patch === 0 && minor === 0) {
      type = 'major';
    }

    return { major, minor, patch, type };
  }

  public createClusters(nodes: ReleaseNode[]): ReleaseNode[] {
    if (!this.timelineScale || nodes.length === 0) return nodes;

    const miniNodes = nodes.filter((n) => n.isMiniNode);
    const regularNodes = nodes.filter((n) => !n.isMiniNode);

    const CLUSTER_THRESHOLD = 150;
    const MIN_SPACING_OUTSIDE = 60;
    const MIN_SPACING_INSIDE = 70;

    const clusterStartDate = new Date(this.timelineScale.latestReleaseDate);
    clusterStartDate.setMonth(clusterStartDate.getMonth() - 4);

    const { clusters, consumedNodeIds } = this.findClusterComponents(regularNodes, clusterStartDate, CLUSTER_THRESHOLD);

    const finalNodes = this.buildClusteredNodeList(regularNodes, clusters, consumedNodeIds);

    finalNodes.push(...miniNodes);

    finalNodes.sort((a, b) => a.position.x - b.position.x);

    return this.applyTimelineSpacing(finalNodes, clusterStartDate, MIN_SPACING_OUTSIDE, MIN_SPACING_INSIDE);
  }

  /**
   * Expands a cluster node into its individual nodes with proper spacing.
   * Centers the expanded nodes around the cluster position.
   * Gives extra spacing to nightly releases due to their longer labels.
   */
  public expandCluster(clusterNode: ReleaseNode): ReleaseNode[] {
    if (!clusterNode.isCluster || !clusterNode.clusteredNodes || clusterNode.clusteredNodes.length === 0) {
      return [clusterNode];
    }

    const BASE_SPACING = 60;
    const SNAPSHOT_EXTRA_SPACING = 30;
    const clusteredNodes = clusterNode.clusteredNodes;
    const centerX = clusterNode.position.x;
    const centerY = clusterNode.position.y;

    const positions: number[] = [];
    let currentX = 0;

    for (let index = 0; index < clusteredNodes.length; index++) {
      const node = clusteredNodes[index];
      const isNightly = this.isNightlyRelease(node.label);
      const previousNode = index > 0 ? clusteredNodes[index - 1] : null;
      const previousIsNightly = previousNode ? this.isNightlyRelease(previousNode.label) : false;

      if (index === 0) {
        positions.push(0);
      } else {
        let spacing = BASE_SPACING;
        if (isNightly || previousIsNightly) {
          spacing += SNAPSHOT_EXTRA_SPACING;
        }
        currentX += spacing;
        positions.push(currentX);
      }
    }

    const totalWidth = positions.at(-1) ?? 0;
    const startX = centerX - totalWidth / 2;

    return clusteredNodes.map((node, index) => ({
      ...node,
      position: {
        x: startX + positions[index],
        y: centerY,
      },
    }));
  }

  /**
   * Iterates through the grouped releases and keeps only the latest nightly release
   * for any branch that contains more than one nightly release.
   * This assumes the releases are already sorted by date (ascending).
   */
  private removeDuplicateNightlies(groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>): void {
    for (const [branchName, releases] of groupedByBranch.entries()) {
      const nightlyReleases = releases.filter((r) => this.isNightlyRelease(r.name));

      if (nightlyReleases.length > 1) {
        const latestNightly = nightlyReleases.at(-1)!;

        const filteredReleases = releases.filter((r) => !this.isNightlyRelease(r.name) || r.id === latestNightly.id);

        groupedByBranch.set(branchName, filteredReleases);
      }
    }
  }

  /**
   * Removes nightly releases with versions same or lower than the previous release.
   * Nightlies are removed from the end of each branch array.
   */
  private filterLowVersionNightliesFromBranches(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
  ): void {
    for (const releases of groupedByBranch.values()) {
      this.removeInvalidNightliesFromBranch(releases);
    }
  }

  /**
   * Removes invalid nightly releases from a single branch.
   */
  private removeInvalidNightliesFromBranch(releases: (Release & { publishedAt: Date })[]): void {
    while (releases.length > 1) {
      const lastRelease = releases.at(-1);
      if (!lastRelease || !this.isNightlyRelease(lastRelease.name)) break;

      const previousRelease = this.findPreviousNonNightlyInArray(releases);
      if (!previousRelease) break;

      if (this.shouldRemoveNightly(lastRelease.name, previousRelease.name)) {
        releases.pop();
      } else {
        break;
      }
    }
  }

  /**
   * Finds the previous non-nightly release in an array.
   */
  private findPreviousNonNightlyInArray(
    releases: (Release & { publishedAt: Date })[],
  ): (Release & { publishedAt: Date }) | null {
    for (let index = releases.length - 2; index >= 0; index--) {
      if (!this.isNightlyRelease(releases[index].name)) {
        return releases[index];
      }
    }
    return null;
  }

  /**
   * Determines if a nightly release should be removed based on version comparison.
   */
  private shouldRemoveNightly(nightlyName: string, previousName: string): boolean {
    const currentVersion = this.getVersionFromName(nightlyName);
    const previousVersion = this.getVersionFromName(previousName);

    if (!currentVersion || !previousVersion) return false;

    return (
      currentVersion.major < previousVersion.major ||
      (currentVersion.major === previousVersion.major && currentVersion.minor < previousVersion.minor) ||
      (currentVersion.major === previousVersion.major &&
        currentVersion.minor === previousVersion.minor &&
        currentVersion.patch <= previousVersion.patch)
    );
  }

  /**
   * Removes minor release branches entirely if all their nodes are unsupported.
   */
  private pruneUnsupportedMinorBranches(groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>): void {
    for (const [branchName, releases] of groupedByBranch.entries()) {
      if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH || releases.length === 0) {
        continue;
      }

      const firstReleaseNode = this.createReleaseNodes([releases[0]])[0];
      const versionInfo = this.getVersionInfo(firstReleaseNode);

      if (versionInfo?.type === 'minor') {
        const allUnsupported = this.createReleaseNodes(releases).every((node) => this.isUnsupported(node));
        if (allUnsupported) {
          groupedByBranch.delete(branchName);
        }
      }
    }
  }

  /**
   * Filters out unsupported minor releases from the master branch that don't have patch versions.
   * These are versions like v6.1, v7.4 that should be hidden if they don't have supported patches.
   */
  private filterUnsupportedMinorReleases(masterNodes: ReleaseNode[]): ReleaseNode[] {
    return masterNodes.filter((node) => {
      if (node.isMiniNode) {
        return true;
      }

      const versionInfo = this.getVersionInfo(node);

      if (node.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
        return true;
      }

      if (versionInfo?.type === 'major') {
        return true;
      }

      if (versionInfo?.type === 'minor') {
        return !this.isUnsupported(node);
      }

      return true;
    });
  }

  /**
   * Checks if a release is a nightly release based on:
   * 1. Contains "nightly" in the name
   * 2. Matches pattern vX.Y.Z-YYYYMMDD.HHMMSS (nightly)
   */
  private isNightlyRelease(label: string): boolean {
    // NOTE: This checks the *original* release name (passed in from release.name) or the node label
    // If checking the node label, we must check for the DISPLAY term ('snapshot')
    if (label.toLowerCase().includes(ReleaseNodeService.GITHUB_SNAPSHOT_DISPLAY)) {
      return true;
    }

    const timestampPattern = /^v?\d+\.\d+\.\d+-\d{8}\.\d{6}/;
    return timestampPattern.test(label);
  }

  /**
   * Extracts version information from a release name (works with both Release and ReleaseNode).
   */
  private getVersionFromName(name: string): { major: number; minor: number; patch: number } | null {
    const match = name.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
    if (!match) return null;

    const major = Number.parseInt(match[1], 10);
    const minor = Number.parseInt(match[2], 10);
    const patch = Number.parseInt(match[3] ?? '0', 10);

    return { major, minor, patch };
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
      label: this.transformNodeLabel(r),
      branch: r.branch.name,
      publishedAt: r.publishedAt,
      color: '',
      position: { x: 0, y: 0 },
    }));
  }

  /**
   * Transforms the release tagName into the final display label based on a set of rules:
   * 1. Strips "release/" prefix.
   * 2. Handles 'master' nightly releases by extracting version from release.name and converting 'nightly' to 'snapshot'.
   * 3. Converts all other 'nightly' references to 'snapshot'.
   */
  private transformNodeLabel(release: Release): string {
    let label = release.tagName;

    label = label.replace(/^release\//, '');

    const isMasterNightly =
      label.toLowerCase().includes(ReleaseNodeService.GITHUB_MASTER_BRANCH.toLowerCase()) &&
      this.isNightlyRelease(release.name);

    if (isMasterNightly) {
      const match = release.name.match(/^v?(\d+\.\d+)/i);
      label = match
        ? `${match[1]}-${ReleaseNodeService.GITHUB_SNAPSHOT_DISPLAY}`
        : label.replace(
            new RegExp(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE, 'i'),
            ReleaseNodeService.GITHUB_SNAPSHOT_DISPLAY,
          );
    } else {
      label = label.replace(
        new RegExp(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE, 'i'),
        ReleaseNodeService.GITHUB_SNAPSHOT_DISPLAY,
      );
    }

    return label;
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
    if (!this.timelineScale) return;

    for (const node of nodes) {
      const x = this.calculateXPositionFromDate(node.publishedAt, this.timelineScale);
      node.position = { x, y: 0 };
    }
  }

  private getSortedBranches(nodeMap: Map<string, ReleaseNode[]>): [string, ReleaseNode[]][] {
    return [...nodeMap.entries()]
      .filter(([branch]) => branch !== ReleaseNodeService.GITHUB_MASTER_BRANCH)
      .toSorted(([branchA], [branchB]) => {
        const versionA = this.getVersionFromBranchName(branchA);
        const versionB = this.getVersionFromBranchName(branchB);
        if (!versionA || !versionB) return 0;
        if (versionB.major !== versionA.major) return versionB.major - versionA.major;
        return versionB.minor - versionA.minor;
      });
  }

  private getVersionFromBranchName(branchName: string): { major: number; minor: number } | null {
    const match = branchName.match(/(\d+)\.(\d+)/);
    return match && match[1] && match[2]
      ? { major: Number.parseInt(match[1], 10), minor: Number.parseInt(match[2], 10) }
      : null;
  }

  private positionBranches(
    branches: [string, ReleaseNode[]][],
    masterNodes: ReleaseNode[],
    positionedNodes: Map<string, ReleaseNode[]>,
  ): void {
    const Y_SPACING = 90;
    let yLevel = 1;

    for (const [branchName, nodes] of branches) {
      if (nodes.every((n) => this.isUnsupported(n))) continue;

      const miniNode = masterNodes.find((n) => n.originalBranch === branchName && n.isMiniNode);
      if (!miniNode) continue;

      const baseY = yLevel * Y_SPACING;
      this.positionBranchNodes(nodes, baseY, miniNode);
      positionedNodes.set(branchName, nodes);
      yLevel++;
    }
  }

  private positionBranchNodes(nodes: ReleaseNode[], baseY: number, miniNode: ReleaseNode): void {
    if (!this.timelineScale) return;

    for (const node of nodes) {
      const x = this.calculateXPositionFromDate(node.publishedAt, this.timelineScale);
      node.position = { x, y: baseY };
    }

    const MINI_NODE_OFFSET = 40;
    if (nodes.length > 0) {
      miniNode.position.x = nodes[0].position.x - MINI_NODE_OFFSET;
    }
  }

  /**
   * Builds a map from version key (e.g., "8.0" or "8.1") to the parent major/minor release node.
   *
   * FIX: This method now explicitly excludes snapshot/nightly releases to ensure that only
   * genuine minor/major versions are used as parent anchors for support calculations.
   */
  private buildParentVersionMap(allNodes: ReleaseNode[]): Map<string, ReleaseNode> {
    const parentMap = new Map<string, ReleaseNode>();

    for (const node of allNodes) {
      const versionInfo = this.getVersionInfo(node);

      const isSnapshot = node.label.toLowerCase().includes(ReleaseNodeService.GITHUB_SNAPSHOT_DISPLAY);

      if (versionInfo && versionInfo.patch === 0 && !isSnapshot) {
        const key = `${versionInfo.major}.${versionInfo.minor}`;
        parentMap.set(key, node);
      }
    }

    return parentMap;
  }

  private determineColor(
    release: ReleaseNode,
    isPatchVersion: boolean,
    isLatestPatch: boolean,
    parentNode: ReleaseNode | null,
  ): SupportColor {
    if (release.label.toLowerCase().includes(ReleaseNodeService.GITHUB_SNAPSHOT_DISPLAY)) {
      return SupportColors.NIGHTLY;
    }

    if (isPatchVersion && !isLatestPatch) {
      return SupportColors.NONE;
    }

    if (isPatchVersion && parentNode) {
      return this.getPatchColorBasedOnParent(release, parentNode);
    }

    const supportDates = this.getSupportEndDates(release);
    if (!supportDates) return SupportColors.NONE;

    const { fullSupportEnd, securitySupportEnd } = supportDates;
    const now = new Date();

    if (now <= fullSupportEnd) return SupportColors.FULL;
    if (now <= securitySupportEnd) return SupportColors.SECURITY;
    return SupportColors.NONE;
  }

  /**
   * Determines patch color based on when it was published relative to parent's support period
   */
  private getPatchColorBasedOnParent(patch: ReleaseNode, parent: ReleaseNode): SupportColor {
    const versionInfo = this.getVersionInfo(patch);
    if (!versionInfo) return SupportColors.NONE;

    const parentPublishedDate = new Date(parent.publishedAt);
    const patchPublishedDate = new Date(patch.publishedAt);

    let fullSupportMonths: number;
    let securitySupportMonths: number;

    if (versionInfo.minor === 0) {
      fullSupportMonths = 6;
      securitySupportMonths = 12;
    } else {
      fullSupportMonths = 3;
      securitySupportMonths = 6;
    }

    const fullSupportEnd = new Date(parentPublishedDate);
    fullSupportEnd.setMonth(parentPublishedDate.getMonth() + fullSupportMonths);
    const securitySupportEnd = new Date(parentPublishedDate);
    securitySupportEnd.setMonth(parentPublishedDate.getMonth() + securitySupportMonths);

    if (patchPublishedDate <= fullSupportEnd) {
      return SupportColors.FULL;
    } else if (patchPublishedDate <= securitySupportEnd) {
      return SupportColors.SECURITY;
    } else {
      return SupportColors.NONE;
    }
  }

  private isUnsupported(release: ReleaseNode): boolean {
    // Corrected to check for 'snapshot' to ensure nightlies are never considered unsupported
    if (release.label.toLowerCase().includes(ReleaseNodeService.GITHUB_SNAPSHOT_DISPLAY)) {
      return false;
    }

    const supportDates = this.getSupportEndDates(release);
    if (!supportDates) return true;

    return new Date() > supportDates.securitySupportEnd;
  }

  private getSupportEndDates(release: ReleaseNode): { fullSupportEnd: Date; securitySupportEnd: Date } | null {
    const versionInfo = this.getVersionInfo(release);
    if (!versionInfo) return null;

    let fullSupportMonths: number;
    let securitySupportMonths: number;

    const basePublishedDate = new Date(release.publishedAt);

    if (versionInfo.type === 'major') {
      fullSupportMonths = 6;
      securitySupportMonths = 12;
    } else {
      fullSupportMonths = 3;
      securitySupportMonths = 6;
    }

    const fullSupportEnd = new Date(basePublishedDate);
    fullSupportEnd.setMonth(basePublishedDate.getMonth() + fullSupportMonths);
    const securitySupportEnd = new Date(basePublishedDate);
    securitySupportEnd.setMonth(basePublishedDate.getMonth() + securitySupportMonths);

    return { fullSupportEnd, securitySupportEnd };
  }

  /**
   * Calculates the timeline scale based on all nodes' publish dates.
   * Uses a fixed width per quarter for consistent, scalable timeline.
   */
  private calculateTimelineScale(allNodes: ReleaseNode[]): TimelineScale {
    if (allNodes.length === 0) {
      const now = new Date();
      return {
        startDate: now,
        endDate: now,
        pixelsPerDay: 1,
        totalDays: 0,
        quarters: [],
        latestReleaseDate: now,
      };
    }

    const PIXELS_PER_QUARTER = 200;

    const dates = allNodes.map((n) => n.publishedAt.getTime());
    const minTime = Math.min(...dates);
    const maxTime = Math.max(...dates);
    const latestReleaseDate = new Date(maxTime);

    const firstDate = new Date(minTime);
    const startDate = this.getQuarterStart(firstDate);
    startDate.setMonth(startDate.getMonth() - 3);

    const lastDate = new Date(maxTime);
    const endDate = this.getQuarterEnd(lastDate);
    endDate.setMonth(endDate.getMonth() + 3);

    const totalDays = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);

    const AVERAGE_DAYS_PER_QUARTER = 90;
    const pixelsPerDay = PIXELS_PER_QUARTER / AVERAGE_DAYS_PER_QUARTER;

    const quarters = this.generateQuarterMarkers(startDate, endDate, pixelsPerDay);

    return {
      startDate,
      endDate,
      pixelsPerDay,
      totalDays,
      quarters,
      latestReleaseDate,
    };
  }

  private getQuarterStart(date: Date): Date {
    const year = date.getFullYear();
    const month = date.getMonth();
    const quarterStartMonth = Math.floor(month / 3) * 3;
    return new Date(year, quarterStartMonth, 1);
  }

  private getQuarterEnd(date: Date): Date {
    const year = date.getFullYear();
    const month = date.getMonth();
    const quarterStartMonth = Math.floor(month / 3) * 3;
    return new Date(year, quarterStartMonth + 3, 0);
  }

  private generateQuarterMarkers(startDate: Date, endDate: Date, pixelsPerDay: number): QuarterMarker[] {
    const markers: QuarterMarker[] = [];
    const PIXELS_PER_QUARTER = 200; // Same as in calculateTimelineScale
    let currentDate = new Date(this.getQuarterStart(startDate));

    while (currentDate <= endDate) {
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth();
      const quarter = Math.floor(month / 3) + 1;

      const daysSinceStart = (currentDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
      const x = daysSinceStart * pixelsPerDay;
      const labelX = x + PIXELS_PER_QUARTER / 2;

      markers.push({
        label: `Q${quarter} ${year}`,
        date: new Date(currentDate),
        x,
        labelX,
        year,
        quarter,
      });

      currentDate.setMonth(currentDate.getMonth() + 3);
    }

    return markers;
  }

  /**
   * Calculates the X position for a node based on its publish date.
   */
  private calculateXPositionFromDate(publishedAt: Date, scale: TimelineScale): number {
    const daysSinceStart = (publishedAt.getTime() - scale.startDate.getTime()) / (1000 * 60 * 60 * 24);
    return daysSinceStart * scale.pixelsPerDay;
  }

  private findClusterComponents(
    nodes: ReleaseNode[],
    clusterStartDate: Date,
    threshold: number,
  ): { clusters: ReleaseNode[]; consumedNodeIds: Set<string> } {
    const consumedNodeIds = new Set<string>();
    const clusters: ReleaseNode[] = [];

    for (const currentNode of nodes) {
      const inClusterZone = currentNode.publishedAt >= clusterStartDate;

      if (consumedNodeIds.has(currentNode.id) || !inClusterZone) {
        continue;
      }

      consumedNodeIds.add(currentNode.id);

      const currentCluster = this.performBFS(currentNode, nodes, threshold, consumedNodeIds);

      const clusterNode = this.createClusterNode(currentCluster);

      if (clusterNode) {
        clusters.push(clusterNode);
      } else {
        consumedNodeIds.delete(currentNode.id);
      }
    }
    return { clusters, consumedNodeIds };
  }

  private performBFS(
    startNode: ReleaseNode,
    allNodes: ReleaseNode[],
    threshold: number,
    consumedNodeIds: Set<string>,
  ): ReleaseNode[] {
    const clusterNodes: ReleaseNode[] = [];
    const queue: ReleaseNode[] = [startNode];

    while (queue.length > 0) {
      const nodeToSearchFrom = queue.shift()!;
      clusterNodes.push(nodeToSearchFrom);

      for (const potentialNeighbor of allNodes) {
        if (consumedNodeIds.has(potentialNeighbor.id)) continue;

        const gap = Math.abs(potentialNeighbor.position.x - nodeToSearchFrom.position.x);
        if (gap < threshold) {
          consumedNodeIds.add(potentialNeighbor.id);
          queue.push(potentialNeighbor);
        }
      }
    }
    return clusterNodes;
  }

  private createClusterNode(clusterNodes: ReleaseNode[]): ReleaseNode | null {
    if (clusterNodes.length <= 1) {
      return null;
    }

    clusterNodes.sort((a, b) => a.position.x - b.position.x);
    const firstNodeInCluster = clusterNodes[0];

    return {
      id: `cluster-${clusterNodes.map((n) => n.id).join('-')}`,
      label: `${clusterNodes.length}`,
      position: { x: firstNodeInCluster.position.x, y: firstNodeInCluster.position.y },
      color: '#dee2e6',
      branch: firstNodeInCluster.branch,
      publishedAt: firstNodeInCluster.publishedAt,
      isCluster: true,
      clusteredNodes: clusterNodes,
      isExpanded: false,
    };
  }

  private buildClusteredNodeList(
    allNodes: ReleaseNode[],
    clusters: ReleaseNode[],
    consumedNodeIds: Set<string>,
  ): ReleaseNode[] {
    const finalNodes = allNodes.filter((node) => !consumedNodeIds.has(node.id));

    finalNodes.push(...clusters);

    return finalNodes;
  }

  private applyTimelineSpacing(
    nodes: ReleaseNode[],
    clusterStartDate: Date,
    minSpacing: number,
    minSpacingInside: number,
  ): ReleaseNode[] {
    const spacedFinalNodes: ReleaseNode[] = [];

    for (const node of nodes) {
      const previousNode = spacedFinalNodes.at(-1);

      // Skip spacing adjustment for mini nodes - they need to maintain their offset from branch nodes
      if (previousNode && !node.isMiniNode) {
        const gap = node.position.x - previousNode.position.x;

        const nodeInZone = node.publishedAt >= clusterStartDate;
        const previousNodeInZone = previousNode.publishedAt >= clusterStartDate;

        let currentMinSpacing = minSpacing;
        if (nodeInZone || previousNodeInZone || node.isCluster || previousNode.isCluster) {
          currentMinSpacing = minSpacingInside;
        }

        if (gap < currentMinSpacing) {
          node.position.x = previousNode.position.x + currentMinSpacing;
        }
      }

      spacedFinalNodes.push(node);
    }
    return spacedFinalNodes;
  }
}
