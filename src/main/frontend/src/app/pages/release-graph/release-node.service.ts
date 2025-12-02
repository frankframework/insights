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
  LATEST_STABLE: '#30A102',
  CUTTING_EDGE: '#FFD700',
  SUPPORTED: '#007BFF',
  LTS: '#9370DB',
  EOL: '#DC3545',
  HISTORICAL: '#F8F8F8',
  ARCHIVED: '#F8F8F8',
} as const;

interface VersionInfo {
  major: number;
  minor: number;
  patch: number;
  type: 'major' | 'minor' | 'patch';
}

interface SupportDates {
  fullSupportEnd: Date;
  securitySupportEnd: Date;
}

@Injectable({ providedIn: 'root' })
export class ReleaseNodeService {
  private static readonly GITHUB_MASTER_BRANCH: string = 'master';
  private static readonly GITHUB_NIGHTLY_RELEASE: string = 'nightly';
  private static readonly PIXELS_PER_QUARTER: number = 200;

  public timelineScale: TimelineScale | null = null;

  public structureReleaseData(releases: Release[]): Map<string, ReleaseNode[]>[] {
    const hydratedReleases = this.hydrateReleases(releases);
    const groupedByBranch = this.prepareGroupedReleases(hydratedReleases);

    const filteredNodes = this.processMasterBranch(groupedByBranch);
    const branchMaps = this.processBranchReleases(groupedByBranch, filteredNodes);

    this.sortByNightlyAndDate(filteredNodes, (node) => node.label);

    const masterMap = new Map([[ReleaseNodeService.GITHUB_MASTER_BRANCH, filteredNodes]]);
    return [masterMap, ...branchMaps];
  }

  public calculateReleaseCoordinates(structuredGroups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
    if (structuredGroups.length === 0) {
      return new Map();
    }

    const nodeMap = this.flattenGroupMaps(structuredGroups);

    const allNodes: ReleaseNode[] = [];
    for (const nodes of nodeMap.values()) {
      allNodes.push(...nodes);
    }

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
    const allNodes = this.collectAllNonMiniNodes(releaseGroups);
    const branchGroups = this.groupNodesByBranch(allNodes);
    this.sortBranchGroupsByDate(branchGroups);
    this.initializeAllColorsAsHistorical(allNodes);

    this.assignEOLColors(branchGroups);
    this.assignSupportedColors(branchGroups);
    this.assignLTSColors(allNodes, branchGroups);
    this.assignCuttingEdgeColor(branchGroups);
    this.assignLatestStableColors(allNodes, branchGroups);
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

  public applyMinimumSpacing(nodes: ReleaseNode[]): ReleaseNode[] {
    if (!this.timelineScale || nodes.length === 0) return nodes;

    const miniNodes = nodes.filter((n) => n.isMiniNode);
    const regularNodes = nodes.filter((n) => !n.isMiniNode);

    const oneYearAgoDate = new Date(this.timelineScale.latestReleaseDate);
    oneYearAgoDate.setFullYear(oneYearAgoDate.getFullYear() - 1);

    const nodesByBranch = this.groupNodesByBranch(regularNodes);
    this.applyMinimumSpacingToLastYear(nodesByBranch, oneYearAgoDate);

    const finalNodes = [...regularNodes, ...miniNodes];
    finalNodes.sort((a, b) => a.position.x - b.position.x);

    return finalNodes;
  }

  private applyMinimumSpacingToLastYear(nodesByBranch: Map<string, ReleaseNode[]>, oneYearAgoDate: Date): void {
    const MIN_SPACING = 70;

    for (const branchNodes of nodesByBranch.values()) {
      branchNodes.sort((a, b) => a.position.x - b.position.x);

      const startIndex = this.findFirstLastYearIndex(branchNodes, oneYearAgoDate);
      if (startIndex === -1) continue;

      this.adjustNodeSpacing(branchNodes, startIndex, MIN_SPACING);
    }
  }

  private findFirstLastYearIndex(nodes: ReleaseNode[], oneYearAgoDate: Date): number {
    for (const [index, node] of nodes.entries()) {
      if (node.publishedAt >= oneYearAgoDate) {
        return index;
      }
    }
    return -1;
  }

  private adjustNodeSpacing(nodes: ReleaseNode[], startIndex: number, minSpacing: number): void {
    for (let index = startIndex + 1; index < nodes.length; index++) {
      const previousNode = nodes[index - 1];
      const currentNode = nodes[index];

      const gap = currentNode.position.x - previousNode.position.x;
      if (gap < minSpacing) {
        const adjustment = minSpacing - gap;
        for (let index_ = index; index_ < nodes.length; index_++) {
          nodes[index_].position.x += adjustment;
        }
      }
    }
  }

  private hydrateReleases(releases: Release[]): (Release & { publishedAt: Date })[] {
    return releases.map((r) => ({
      ...r,
      publishedAt: new Date(r.publishedAt),
    }));
  }

  private prepareGroupedReleases(
    hydratedReleases: (Release & { publishedAt: Date })[],
  ): Map<string, (Release & { publishedAt: Date })[]> {
    const groupedByBranch = this.groupReleasesByBranch(hydratedReleases);
    this.sortGroupedReleases(groupedByBranch);
    this.removeDuplicateNightlies(groupedByBranch);
    this.pruneHistoricalBranchesWithoutNightly(groupedByBranch);
    this.filterLowVersionNightliesFromBranches(groupedByBranch);
    return groupedByBranch;
  }

  private processMasterBranch(groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>): ReleaseNode[] {
    const masterReleases = groupedByBranch.get(ReleaseNodeService.GITHUB_MASTER_BRANCH) ?? [];
    const nodes = this.createReleaseNodes(masterReleases);
    const filteredNodes = this.filterUnsupportedMinorReleases(nodes);
    groupedByBranch.delete(ReleaseNodeService.GITHUB_MASTER_BRANCH);
    return filteredNodes;
  }

  private processBranchReleases(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
    filteredNodes: ReleaseNode[],
  ): Map<string, ReleaseNode[]>[] {
    const branchMaps: Map<string, ReleaseNode[]>[] = [];

    for (const [branchName, branchReleases] of groupedByBranch.entries()) {
      if (branchReleases.length === 0) continue;

      const branchNodes = this.createReleaseNodes(branchReleases);
      const miniNode = this.createMiniNode(branchNodes[0], branchName);

      filteredNodes.push(miniNode);
      branchMaps.push(new Map([[branchName, branchNodes]]));
    }

    return branchMaps;
  }

  private createMiniNode(firstBranchNode: ReleaseNode, branchName: string): ReleaseNode {
    return {
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
  }

  private collectAllNonMiniNodes(releaseGroups: Map<string, ReleaseNode[]>): ReleaseNode[] {
    const allNodes: ReleaseNode[] = [];
    for (const nodes of releaseGroups.values()) {
      allNodes.push(...nodes.filter((n) => !n.isMiniNode));
    }
    return allNodes;
  }

  private groupNodesByBranch(nodes: ReleaseNode[]): Map<string, ReleaseNode[]> {
    const branchGroups = new Map<string, ReleaseNode[]>();
    for (const node of nodes) {
      if (!branchGroups.has(node.branch)) {
        branchGroups.set(node.branch, []);
      }
      branchGroups.get(node.branch)!.push(node);
    }
    return branchGroups;
  }

  private sortBranchGroupsByDate(branchGroups: Map<string, ReleaseNode[]>): void {
    for (const nodes of branchGroups.values()) {
      nodes.sort((a, b) => a.publishedAt.getTime() - b.publishedAt.getTime());
    }
  }

  private initializeAllColorsAsHistorical(nodes: ReleaseNode[]): void {
    for (const node of nodes) {
      node.color = SupportColors.HISTORICAL;
    }
  }

  private assignEOLColors(branchGroups: Map<string, ReleaseNode[]>): void {
    for (const [branchName, nodes] of branchGroups) {
      if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH) continue;

      if (this.isEOLBranch(nodes)) {
        this.colorBranchLatestReleases(nodes, SupportColors.EOL);
      }
    }
  }

  private assignSupportedColors(branchGroups: Map<string, ReleaseNode[]>): void {
    for (const [branchName, nodes] of branchGroups) {
      if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH) continue;

      if (this.isBranchSupported(nodes)) {
        this.colorBranchLatestReleases(nodes, SupportColors.SUPPORTED);
      }
    }
  }

  private assignLTSColors(allNodes: ReleaseNode[], branchGroups: Map<string, ReleaseNode[]>): void {
    const latestLTS = this.findLatestLTS(allNodes);
    if (!latestLTS) return;

    const versionInfo = this.getVersionInfo(latestLTS);
    if (!versionInfo) return;

    const ltsBranchKey = `${versionInfo.major}.${versionInfo.minor}`;
    this.colorMatchingLTSBranches(branchGroups, ltsBranchKey, versionInfo);
  }

  private colorMatchingLTSBranches(
    branchGroups: Map<string, ReleaseNode[]>,
    ltsBranchKey: string,
    versionInfo: VersionInfo,
  ): void {
    for (const [branchName, nodes] of branchGroups) {
      if (!branchName.includes(ltsBranchKey) && branchName !== ReleaseNodeService.GITHUB_MASTER_BRANCH) {
        continue;
      }

      this.colorLTSVersionMatches(nodes, versionInfo);
    }
  }

  private colorLTSVersionMatches(nodes: ReleaseNode[], targetVersion: VersionInfo): void {
    const lastRelease = this.findLastNonNightlyRelease(nodes);
    if (!lastRelease) return;

    const lastReleaseVersion = this.getVersionInfo(lastRelease);
    if (!this.versionsMatch(lastReleaseVersion, targetVersion)) return;

    lastRelease.color = SupportColors.LTS;

    const lastNightly = this.findLatestNightly(nodes);
    if (lastNightly) {
      const nightlyVersion = this.getVersionInfo(lastNightly);
      if (this.versionsMatch(nightlyVersion, targetVersion)) {
        lastNightly.color = SupportColors.LTS;
      }
    }
  }

  private versionsMatch(version1: VersionInfo | null, version2: VersionInfo): boolean {
    return version1 !== null && version1.major === version2.major && version1.minor === version2.minor;
  }

  private assignCuttingEdgeColor(branchGroups: Map<string, ReleaseNode[]>): void {
    const masterNodes = branchGroups.get(ReleaseNodeService.GITHUB_MASTER_BRANCH);
    if (!masterNodes) return;

    const cuttingEdge = this.findLatestNightly(masterNodes);
    if (cuttingEdge) {
      cuttingEdge.color = SupportColors.CUTTING_EDGE;
    }
  }

  private assignLatestStableColors(allNodes: ReleaseNode[], branchGroups: Map<string, ReleaseNode[]>): void {
    const latestStable = this.findLatestStable(allNodes);
    if (!latestStable) return;

    const latestStableVersion = this.getVersionInfo(latestStable);
    const isLTS = latestStableVersion?.type === 'major';

    if (!isLTS) {
      this.colorLatestStableBranch(latestStable.branch, branchGroups);
    }
  }

  private colorLatestStableBranch(branchName: string, branchGroups: Map<string, ReleaseNode[]>): void {
    const latestStableBranch = branchGroups.get(branchName);
    if (!latestStableBranch) return;

    this.colorBranchLatestReleases(latestStableBranch, SupportColors.LATEST_STABLE);
  }

  private colorBranchLatestReleases(nodes: ReleaseNode[], color: string): void {
    const lastRelease = this.findLastNonNightlyRelease(nodes);
    const lastNightly = this.findLatestNightly(nodes);

    if (lastRelease) {
      lastRelease.color = color;
    }
    if (lastNightly) {
      lastNightly.color = color;
    }
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
   * Removes branches that are historically dead.
   * Logic:
   * 1. If it has an active Nightly -> KEEP (Active development).
   * 2. If NO Nightly -> Check the SUPPORT status of the BRANCH ROOT (the .0 release).
   * If the .0 release is unsupported, the whole branch is considered historical/skipped,
   * even if a recent patch was released.
   */
  private pruneHistoricalBranchesWithoutNightly(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
  ): void {
    for (const [branchName, releases] of groupedByBranch.entries()) {
      if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH) {
        continue;
      }

      if (releases.length === 0) continue;

      const latestByDate = releases.reduce((previous, current) =>
        previous.publishedAt > current.publishedAt ? previous : current,
      );

      if (this.isNightlyRelease(latestByDate.name)) {
        continue;
      }

      let rootRelease = releases.find((r) => r.name.endsWith('.0') || r.tagName.endsWith('.0'));

      if (!rootRelease) {
        rootRelease = releases.reduce((previous, current) =>
          previous.publishedAt < current.publishedAt ? previous : current,
        );
      }

      const rootNode: ReleaseNode = {
        id: rootRelease.id,
        label: this.transformNodeLabel(rootRelease),
        branch: rootRelease.branch.name,
        publishedAt: rootRelease.publishedAt,
        position: { x: 0, y: 0 },
        color: '',
      };

      if (this.isUnsupported(rootNode)) {
        groupedByBranch.delete(branchName);
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
    if (label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
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
   * 2. Formats nightly releases as vX.Y.X-nightly using the version from release.name.
   */
  private transformNodeLabel(release: Release): string {
    let label = release.tagName;

    label = label.replace(/^release\//, '');

    const isNightly = this.isNightlyRelease(release.name);

    if (isNightly) {
      const match = release.name.match(/^v?(\d+\.\d+\.\d+)/i);
      if (match) {
        label = `v${match[1]}-${ReleaseNodeService.GITHUB_NIGHTLY_RELEASE}`;
      }
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
   * Calculates the timeline scale based on all nodes' publish dates.
   * Uses a fixed width per quarter for consistent, scalable timeline.
   */
  private calculateTimelineScale(allNodes: ReleaseNode[]): TimelineScale {
    if (allNodes.length === 0) {
      return this.createEmptyTimelineScale();
    }

    const { minTime, maxTime } = this.getTimeRange(allNodes);
    const latestReleaseDate = new Date(maxTime);

    const startDate = this.calculateStartDate(minTime);
    const endDate = this.calculateEndDate(maxTime);

    const totalDays = this.calculateDaysBetween(startDate, endDate);
    const pixelsPerDay = this.calculatePixelsPerDay();
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

  private createEmptyTimelineScale(): TimelineScale {
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

  private getTimeRange(allNodes: ReleaseNode[]): { minTime: number; maxTime: number } {
    const dates = allNodes.map((n) => n.publishedAt.getTime());
    return {
      minTime: Math.min(...dates),
      maxTime: Math.max(...dates),
    };
  }

  private calculateStartDate(minTime: number): Date {
    const firstDate = new Date(minTime);
    const startDate = this.getQuarterStart(firstDate);
    startDate.setMonth(startDate.getMonth() - 3);
    return startDate;
  }

  private calculateEndDate(maxTime: number): Date {
    const lastDate = new Date(maxTime);
    const endDate = this.getQuarterEnd(lastDate);
    endDate.setMonth(endDate.getMonth() + 3);
    return endDate;
  }

  private calculateDaysBetween(startDate: Date, endDate: Date): number {
    return (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
  }

  private calculatePixelsPerDay(): number {
    const AVERAGE_DAYS_PER_QUARTER = 90;
    return ReleaseNodeService.PIXELS_PER_QUARTER / AVERAGE_DAYS_PER_QUARTER;
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
    let currentDate = new Date(this.getQuarterStart(startDate));

    while (currentDate <= endDate) {
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth();
      const quarter = Math.floor(month / 3) + 1;

      const daysSinceStart = (currentDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
      const x = daysSinceStart * pixelsPerDay;
      const labelX = x + ReleaseNodeService.PIXELS_PER_QUARTER / 2;

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

  private findLatestStable(nodes: ReleaseNode[]): ReleaseNode | null {
    const nonNightly = nodes.filter((n) => !this.isNightlyRelease(n.label));
    if (nonNightly.length === 0) return null;

    const nodesWithVersions = nonNightly
      .map((node) => ({ node, version: this.getVersionInfo(node) }))
      .filter((item) => item.version !== null);

    if (nodesWithVersions.length === 0) return null;

    return nodesWithVersions.reduce((latest, current) =>
      this.compareVersions(current.version!, latest.version!) > 0 ? current : latest,
    ).node;
  }

  private compareVersions(v1: VersionInfo, v2: VersionInfo): number {
    if (v1.major !== v2.major) return v1.major - v2.major;
    if (v1.minor !== v2.minor) return v1.minor - v2.minor;
    return v1.patch - v2.patch;
  }

  private findLatestNightly(nodes: ReleaseNode[]): ReleaseNode | null {
    const nightlies = nodes.filter((n) => this.isNightlyRelease(n.label));
    if (nightlies.length === 0) return null;

    return nightlies.reduce((latest, node) => {
      return node.publishedAt > latest.publishedAt ? node : latest;
    });
  }

  private findLastNonNightlyRelease(nodes: ReleaseNode[]): ReleaseNode | null {
    const nonNightly = nodes.filter((n) => !this.isNightlyRelease(n.label));
    if (nonNightly.length === 0) return null;

    return nonNightly.reduce((latest, node) => {
      return node.publishedAt > latest.publishedAt ? node : latest;
    });
  }

  private isBranchSupported(nodes: ReleaseNode[]): boolean {
    const firstRelease = nodes.find((n) => !this.isNightlyRelease(n.label));
    if (!firstRelease) return false;

    const versionInfo = this.getVersionInfo(firstRelease);
    if (!versionInfo) return false;

    const supportDates = this.getSupportEndDates(firstRelease);
    if (!supportDates) return false;

    return new Date() <= supportDates.securitySupportEnd;
  }

  private isEOLBranch(nodes: ReleaseNode[]): boolean {
    const hasNightly = nodes.some((n) => this.isNightlyRelease(n.label));
    if (!hasNightly) return false;

    return this.isBranchUnsupported(nodes);
  }

  private isBranchUnsupported(nodes: ReleaseNode[]): boolean {
    const firstRelease = nodes.find((n) => !this.isNightlyRelease(n.label));
    if (!firstRelease) return true;

    const supportDates = this.getSupportEndDates(firstRelease);
    if (!supportDates) return true;

    return new Date() > supportDates.securitySupportEnd;
  }

  private findLatestLTS(nodes: ReleaseNode[]): ReleaseNode | null {
    const majorVersions = nodes.filter((n) => {
      const versionInfo = this.getVersionInfo(n);
      return versionInfo?.type === 'major';
    });

    if (majorVersions.length === 0) return null;

    return majorVersions.reduce((latest, node) => {
      const latestVersion = this.getVersionInfo(latest);
      const nodeVersion = this.getVersionInfo(node);

      if (!latestVersion || !nodeVersion) return latest;

      if (nodeVersion.major > latestVersion.major) return node;
      return latest;
    });
  }

  private isUnsupported(release: ReleaseNode): boolean {
    if (release.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
      return false;
    }

    const supportDates = this.getSupportEndDates(release);
    if (!supportDates) return true;

    return new Date() > supportDates.securitySupportEnd;
  }

  private getSupportEndDates(release: ReleaseNode): SupportDates | null {
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
}
