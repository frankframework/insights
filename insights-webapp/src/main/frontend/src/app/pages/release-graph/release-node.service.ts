import { Injectable } from '@angular/core';
import { Release } from '../../services/release.service';
import {
  createLineRange,
  createOpenEndedRange,
  isVersionInRanges,
  mergeVersionRanges,
  VersionRange,
} from '../../pipes/release-range';

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
  BLEEDING_EDGE: '#FFD700',
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

interface ReleaseLine {
  major: number;
  minor: number;
}

@Injectable({ providedIn: 'root' })
export class ReleaseNodeService {
  public static readonly GITHUB_NIGHTLY_RELEASE: string = 'nightly';

  private static readonly GITHUB_MASTER_BRANCH: string = 'master';
  private static readonly PIXELS_PER_QUARTER: number = 200;
  private static readonly KEEP_LATEST_LTS_COUNT: number = 2;

  public timelineScale: TimelineScale | null = null;

  public structureReleaseData(releases: Release[], ranges: VersionRange[] = []): Map<string, ReleaseNode[]>[] {
    const hydratedReleases = this.hydrateReleases(releases);
    const branchOriginDates = this.captureBranchOriginDates(hydratedReleases);
    const groupedByBranch = this.prepareGroupedReleases(hydratedReleases, ranges);

    const filteredNodes = this.processMasterBranch(groupedByBranch, ranges);
    const branchMaps = this.processBranchReleases(groupedByBranch, filteredNodes, branchOriginDates);

    this.sortByNightlyAndDate(filteredNodes, (node) => node.label);

    const masterMap = new Map([[ReleaseNodeService.GITHUB_MASTER_BRANCH, filteredNodes]]);
    return [masterMap, ...branchMaps];
  }

  public getDefaultRanges(releases: Release[]): VersionRange[] {
    const hydratedReleases = this.hydrateReleases(releases);
    const groupedByBranch = this.groupReleasesByBranch(hydratedReleases);
    this.sortGroupedReleases(groupedByBranch);
    this.removeDuplicateNightlies(groupedByBranch);
    this.pruneHistoricalBranchesWithoutNightly(groupedByBranch);

    const visibleLines = this.collectBranchReleaseLines(groupedByBranch);
    const supportedLineKeys = this.collectSupportedLineKeys(hydratedReleases);
    const openEndedLine = this.findOpenEndedLine(visibleLines, supportedLineKeys);

    if (!openEndedLine) return [{ from: null, to: null }];

    const openEndedKey = this.toReleaseLineKey(openEndedLine);
    const lineRanges = visibleLines
      .filter((line) => this.toReleaseLineKey(line) < openEndedKey)
      .map((line) => createLineRange(line.major, line.minor));

    return mergeVersionRanges(lineRanges, [createOpenEndedRange(openEndedLine.major, openEndedLine.minor)]);
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
    this.assignBleedingEdgeColor(branchGroups);
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
    const MIN_SPACING = 65;

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

  private captureBranchOriginDates(hydratedReleases: (Release & { publishedAt: Date })[]): Map<string, Date> {
    const originDates = new Map<string, Date>();
    for (const release of hydratedReleases) {
      const branchName = release.branch.name;
      const existing = originDates.get(branchName);
      if (!existing || release.publishedAt < existing) {
        originDates.set(branchName, release.publishedAt);
      }
    }
    return originDates;
  }

  private prepareGroupedReleases(
    hydratedReleases: (Release & { publishedAt: Date })[],
    ranges: VersionRange[],
  ): Map<string, (Release & { publishedAt: Date })[]> {
    const groupedByBranch = this.groupReleasesByBranch(hydratedReleases);
    this.sortGroupedReleases(groupedByBranch);
    this.removeDuplicateNightlies(groupedByBranch);
    this.applyRangeVisibility(groupedByBranch, ranges);
    this.filterLowVersionNightliesFromBranches(groupedByBranch);
    return groupedByBranch;
  }

  private applyRangeVisibility(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
    ranges: VersionRange[],
  ): void {
    if (ranges.length === 0) return;

    for (const [branchName, releases] of groupedByBranch.entries()) {
      if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH) continue;

      const releasesInRange = releases.filter((release) => this.isReleaseInRange(release, ranges));
      if (releasesInRange.length > 0) {
        groupedByBranch.set(branchName, releasesInRange);
      } else {
        groupedByBranch.delete(branchName);
      }
    }
  }

  private isReleaseInRange(release: Release, ranges: VersionRange[]): boolean {
    return this.isVersionOfInRange(release.name, ranges);
  }

  private isNodeInRange(node: ReleaseNode, ranges: VersionRange[]): boolean {
    return this.isVersionOfInRange(node.label, ranges);
  }

  private isVersionOfInRange(label: string, ranges: VersionRange[]): boolean {
    const versionInfo = this.getVersionInfo({ label } as ReleaseNode);
    if (!versionInfo) return false;

    return isVersionInRanges(ranges, versionInfo.major, versionInfo.minor, versionInfo.patch);
  }

  private processMasterBranch(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
    ranges: VersionRange[],
  ): ReleaseNode[] {
    const masterReleases = groupedByBranch.get(ReleaseNodeService.GITHUB_MASTER_BRANCH) ?? [];
    const nodes = this.createReleaseNodes(masterReleases);
    const filteredNodes = this.filterMasterReleases(nodes, ranges);
    groupedByBranch.delete(ReleaseNodeService.GITHUB_MASTER_BRANCH);
    return filteredNodes;
  }

  private collectBranchReleaseLines(groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>): ReleaseLine[] {
    const lines: ReleaseLine[] = [];

    for (const [branchName, releases] of groupedByBranch.entries()) {
      if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH || releases.length === 0) continue;

      const versionInfo = this.getVersionInfo(this.buildBranchRootNode(releases));
      if (versionInfo) {
        lines.push({ major: versionInfo.major, minor: versionInfo.minor });
      }
    }

    return this.sortReleaseLines(lines);
  }

  private collectSupportedLineKeys(hydratedReleases: (Release & { publishedAt: Date })[]): Set<number> {
    const releasesByLine = new Map<number, (Release & { publishedAt: Date })[]>();

    for (const release of hydratedReleases) {
      if (this.isNightlyRelease(release.name)) continue;

      const versionInfo = this.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!versionInfo) continue;

      const lineKey = this.toReleaseLineKey(versionInfo);
      const lineReleases = releasesByLine.get(lineKey) ?? [];
      lineReleases.push(release);
      releasesByLine.set(lineKey, lineReleases);
    }

    const supportedLineKeys = new Set<number>();
    for (const [lineKey, lineReleases] of releasesByLine.entries()) {
      if (!this.isUnsupported(this.buildBranchRootNode(lineReleases))) {
        supportedLineKeys.add(lineKey);
      }
    }

    return supportedLineKeys;
  }

  private findOpenEndedLine(visibleLines: ReleaseLine[], supportedLineKeys: Set<number>): ReleaseLine | undefined {
    let openEndedLine: ReleaseLine | undefined;

    for (let index = visibleLines.length - 1; index >= 0; index--) {
      if (!supportedLineKeys.has(this.toReleaseLineKey(visibleLines[index]))) break;
      openEndedLine = visibleLines[index];
    }

    return openEndedLine ?? visibleLines.at(-1);
  }

  private sortReleaseLines(lines: ReleaseLine[]): ReleaseLine[] {
    const uniqueLines = new Map(lines.map((line) => [this.toReleaseLineKey(line), line]));
    return [...uniqueLines.values()].toSorted((a, b) => this.toReleaseLineKey(a) - this.toReleaseLineKey(b));
  }

  private toReleaseLineKey(line: ReleaseLine): number {
    return line.major * 100_000 + line.minor;
  }

  private processBranchReleases(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
    filteredNodes: ReleaseNode[],
    branchOriginDates: Map<string, Date>,
  ): Map<string, ReleaseNode[]>[] {
    const branchMaps: Map<string, ReleaseNode[]>[] = [];

    for (const [branchName, branchReleases] of groupedByBranch.entries()) {
      if (branchReleases.length === 0) continue;

      const branchNodes = this.createReleaseNodes(branchReleases);
      const originDate = branchOriginDates.get(branchName) ?? branchNodes[0].publishedAt;
      const miniNode = this.createMiniNode(branchNodes[0], branchName, originDate);

      filteredNodes.push(miniNode);
      branchMaps.push(new Map([[branchName, branchNodes]]));
    }

    return branchMaps;
  }

  private createMiniNode(firstBranchNode: ReleaseNode, branchName: string, originDate: Date): ReleaseNode {
    return {
      id: `mini-${firstBranchNode.id}`,
      label: '',
      branch: ReleaseNodeService.GITHUB_MASTER_BRANCH,
      publishedAt: originDate,
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

  private assignBleedingEdgeColor(branchGroups: Map<string, ReleaseNode[]>): void {
    const masterNodes = branchGroups.get(ReleaseNodeService.GITHUB_MASTER_BRANCH);
    if (!masterNodes) return;

    const bleedingEdge = this.findLatestNightly(masterNodes);
    if (bleedingEdge) {
      bleedingEdge.color = SupportColors.BLEEDING_EDGE;
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
   * Returns the set of branch names for the N most recent major (x.0) version branches.
   * These are always kept visible regardless of support status or nightly activity.
   */
  private findLatestMajorBranchNames(branchNames: string[]): Set<string> {
    const majorBranches = branchNames
      .map((name) => ({ name, version: this.getVersionFromBranchName(name) }))
      .filter(
        (item): item is { name: string; version: { major: number; minor: number } } =>
          item.version !== null && item.version.minor === 0,
      )
      .toSorted((a, b) => b.version.major - a.version.major)
      .slice(0, ReleaseNodeService.KEEP_LATEST_LTS_COUNT);

    return new Set(majorBranches.map((b) => b.name));
  }

  private pruneHistoricalBranchesWithoutNightly(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
  ): void {
    const protectedMajors = this.findLatestMajorBranchNames([...groupedByBranch.keys()]);
    const candidates = this.collectUnsupportedPruneCandidates(groupedByBranch, protectedMajors);
    const showcaseBranch = this.findMostRecentlyUnsupportedBranchName(candidates);

    for (const { branchName } of candidates) {
      if (branchName !== showcaseBranch) {
        groupedByBranch.delete(branchName);
      }
    }
  }

  private collectUnsupportedPruneCandidates(
    groupedByBranch: Map<string, (Release & { publishedAt: Date })[]>,
    protectedMajors: Set<string>,
  ): { branchName: string; rootRelease: ReleaseNode }[] {
    const candidates: { branchName: string; rootRelease: ReleaseNode }[] = [];

    for (const [branchName, releases] of groupedByBranch.entries()) {
      if (!this.isPruneEligibleBranch(branchName, releases, protectedMajors)) continue;

      const rootNode = this.buildBranchRootNode(releases);
      if (this.isUnsupported(rootNode)) {
        candidates.push({ branchName, rootRelease: rootNode });
      }
    }

    return candidates;
  }

  private isPruneEligibleBranch(
    branchName: string,
    releases: (Release & { publishedAt: Date })[],
    protectedMajors: Set<string>,
  ): boolean {
    if (branchName === ReleaseNodeService.GITHUB_MASTER_BRANCH) return false;
    if (protectedMajors.has(branchName)) return false;

    return releases.length > 0;
  }

  private buildBranchRootNode(releases: (Release & { publishedAt: Date })[]): ReleaseNode {
    let rootRelease = releases.find((release) => release.name.endsWith('.0') || release.tagName.endsWith('.0'));

    if (!rootRelease) {
      rootRelease = releases.reduce((previous, current) =>
        previous.publishedAt < current.publishedAt ? previous : current,
      );
    }

    return {
      id: rootRelease.id,
      label: this.transformNodeLabel(rootRelease),
      branch: rootRelease.branch.name,
      publishedAt: rootRelease.publishedAt,
      position: { x: 0, y: 0 },
      color: '',
    };
  }

  /**
   * Trims the master timeline down to the ranges. Major releases and nightlies stay whatever the
   * ranges say: they are the backbone every branch row hangs off. The rest, the minor releases
   * like v6.1 or v7.4, is only drawn when a range asks for it.
   */
  private filterMasterReleases(masterNodes: ReleaseNode[], ranges: VersionRange[]): ReleaseNode[] {
    return masterNodes.filter((node) => {
      if (node.isMiniNode) {
        return true;
      }

      if (node.label.toLowerCase().includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
        return true;
      }

      if (this.getVersionInfo(node)?.type === 'major') {
        return true;
      }

      return this.isNodeInRange(node, ranges);
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
   * 1. Strips"release/" prefix.
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
    miniNode.position.x = this.calculateXPositionFromDate(miniNode.publishedAt, this.timelineScale) - MINI_NODE_OFFSET;
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

  private findMostRecentlyUnsupportedBranchName(
    candidates: { branchName: string; rootRelease: ReleaseNode }[],
  ): string | null {
    const now = new Date();
    let mostRecent: { branchName: string; end: Date } | null = null;

    for (const { branchName, rootRelease } of candidates) {
      const supportDates = this.getSupportEndDates(rootRelease);
      if (!supportDates || supportDates.securitySupportEnd > now) continue;

      if (!mostRecent || supportDates.securitySupportEnd > mostRecent.end) {
        mostRecent = { branchName, end: supportDates.securitySupportEnd };
      }
    }

    return mostRecent?.branchName ?? null;
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
