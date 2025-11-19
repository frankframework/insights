import { Injectable, inject } from '@angular/core';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';

export interface ReleaseLink {
  id: string;
  source: string;
  target: string;
  isGap?: boolean;
  isFadeIn?: boolean;
}

export interface SkipNode {
  id: string;
  x: number;
  y: number;
  skippedCount: number;
  skippedVersions: string[];
  label: string;
  isMiniNode?: boolean;
}

export interface Release {
  id: string;
  name: string;
  branch: { name: string };
}

interface SkipNodeContext {
  masterNodes: ReleaseNode[];
  allNodes: ReleaseNode[];
}

@Injectable({ providedIn: 'root' })
export class ReleaseLinkService {
  private static readonly GITHUB_MASTER_BRANCH: string = 'master';
  private nodeService = inject(ReleaseNodeService);

  public createLinks(structuredGroups: Map<string, ReleaseNode[]>[], skipNodes: SkipNode[]): ReleaseLink[] {
    if (structuredGroups.length === 0) return [];

    const masterNodes = structuredGroups[0].get(ReleaseLinkService.GITHUB_MASTER_BRANCH) ?? [];
    if (masterNodes.length === 0) return [];

    return [
      ...this.createIntraBranchLinks(masterNodes),
      ...this.createSubBranchLinks(structuredGroups.slice(1), masterNodes),
      ...this.createSpecialLinks(masterNodes, skipNodes),
    ];
  }

  public createSkipNodes(structuredGroups: Map<string, ReleaseNode[]>[], allReleases: Release[]): SkipNode[] {
    const context = this.validateAndPrepareSkipNodeContext(structuredGroups);
    if (!context) return [];

    const skipNodes: SkipNode[] = [];

    this.addBetweenReleaseSkipNodes(context, allReleases, skipNodes);
    this.addInitialSkipNode(context, allReleases, skipNodes);

    return skipNodes;
  }

  public createSkipNodeLinks(skipNodes: SkipNode[], masterNodes: ReleaseNode[]): ReleaseLink[] {
    const links: ReleaseLink[] = [];

    for (const skipNode of skipNodes) {
      if (skipNode.id.startsWith('skip-initial-')) {
        this.handleInitialSkipNode(skipNode, masterNodes, links);
      } else {
        this.handleRegularSkipNode(skipNode, masterNodes, links);
      }
    }

    return links;
  }

  private validateAndPrepareSkipNodeContext(structuredGroups: Map<string, ReleaseNode[]>[]): SkipNodeContext | null {
    if (!structuredGroups?.length) return null;

    const masterNodes = structuredGroups[0]?.get(ReleaseLinkService.GITHUB_MASTER_BRANCH) ?? [];
    if (masterNodes.length === 0) return null;

    const allNodes = this.getAllNodesFlat(structuredGroups);
    return { masterNodes, allNodes };
  }

  private addBetweenReleaseSkipNodes(context: SkipNodeContext, allReleases: Release[], skipNodes: SkipNode[]): void {
    this.createSkipNodesBetweenReleases(context.masterNodes, context.allNodes, allReleases, skipNodes);
  }

  private addInitialSkipNode(context: SkipNodeContext, allReleases: Release[], skipNodes: SkipNode[]): void {
    this.createInitialSkipNode(context.masterNodes, context.allNodes, allReleases, skipNodes);
  }

  private getAllNodesFlat(structuredGroups: Map<string, ReleaseNode[]>[]): ReleaseNode[] {
    return [...structuredGroups.values()]
      .flat()
      .flatMap((nodes) => [...nodes.values()])
      .flat();
  }

  private createSkipNodesBetweenReleases(
    masterNodes: ReleaseNode[],
    allNodes: ReleaseNode[],
    allReleases: Release[],
    skipNodes: SkipNode[],
  ): void {
    for (let index = 0; index < masterNodes.length - 1; index++) {
      const source = masterNodes[index];
      const target = masterNodes[index + 1];

      if (this.isVersionGap(source, target)) {
        const skipNode = this.createSkipNodeBetween(source, target, allNodes, allReleases);
        if (skipNode) {
          skipNodes.push(skipNode);
        }
      }
    }
  }

  private createSkipNodeBetween(
    source: ReleaseNode,
    target: ReleaseNode,
    allNodes: ReleaseNode[],
    allReleases: Release[],
  ): SkipNode | null {
    const skippedReleases = this.findSkippedReleasesBetweenNodes(source, target, allNodes, allReleases);
    const minorReleases = this.filterMinorReleases(skippedReleases);

    if (minorReleases.length === 0) return null;

    const isMiniNode = source.isMiniNode || target.isMiniNode;

    return {
      id: `skip-${source.id}-${target.id}`,
      x: (source.position.x + target.position.x) / 2,
      y: source.position.y,
      skippedCount: minorReleases.length,
      skippedVersions: skippedReleases.map((r) => (r.name.startsWith('v') ? r.name : `v${r.name}`)),
      label: minorReleases.length === 1 ? '1 skipped' : `${minorReleases.length} skipped`,
      isMiniNode,
    };
  }

  private createInitialSkipNode(
    masterNodes: ReleaseNode[],
    allNodes: ReleaseNode[],
    allReleases: Release[],
    skipNodes: SkipNode[],
  ): void {
    if (masterNodes.length === 0) return;

    const firstNode = masterNodes[0];
    const skippedReleases = this.findSkippedReleasesBeforeNode(firstNode, allNodes, allReleases);
    const minorReleases = this.filterMinorReleases(skippedReleases);

    if (minorReleases.length > 0) {
      const timelineScale = this.nodeService.timelineScale;
      if (!timelineScale) return;

      const skipNodeX = firstNode.position.x / 2;

      skipNodes.push({
        id: `skip-initial-${firstNode.id}`,
        x: skipNodeX,
        y: firstNode.position.y,
        skippedCount: minorReleases.length,
        skippedVersions: skippedReleases.map((r) => (r.name.startsWith('v') ? r.name : `v${r.name}`)),
        label: minorReleases.length === 1 ? '1 skipped' : `${minorReleases.length} skipped`,
      });
    }
  }

  private filterMinorReleases(releases: Release[]): Release[] {
    return releases.filter((r) => {
      const info = this.nodeService.getVersionInfo({ label: r.name } as ReleaseNode);
      return info?.type === 'minor' || info?.type === 'major';
    });
  }

  private findSkippedReleasesBetweenNodes(
    source: ReleaseNode,
    target: ReleaseNode,
    allNodes: ReleaseNode[],
    allReleases: Release[],
  ): Release[] {
    const vSource = this.nodeService.getVersionInfo(source);
    const vTarget = this.nodeService.getVersionInfo(target);
    if (!vSource || !vTarget) return [];

    return allReleases.filter((release) => {
      const vRelease = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!vRelease) return false;

      const releaseInGraph = allNodes.some((node) => node.id === release.id);
      if (releaseInGraph) return false;

      return vSource.major === vTarget.major
        ? vRelease.major === vSource.major && vRelease.minor > vSource.minor && vRelease.minor < vTarget.minor
        : (vRelease.major > vSource.major && vRelease.major < vTarget.major) ||
            (vRelease.major === vSource.major && vRelease.minor > vSource.minor) ||
            (vRelease.major === vTarget.major && vRelease.minor < vTarget.minor);
    });
  }

  private findSkippedReleasesBeforeNode(
    firstNode: ReleaseNode,
    allNodes: ReleaseNode[],
    allReleases: Release[],
  ): Release[] {
    const vFirst = this.nodeService.getVersionInfo(firstNode);
    if (!vFirst) return [];

    return allReleases.filter((release) => {
      const vRelease = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!vRelease) return false;

      const releaseInGraph = allNodes.some((node) => node.id === release.id);
      if (releaseInGraph) return false;

      return vRelease.major < vFirst.major || (vRelease.major === vFirst.major && vRelease.minor < vFirst.minor);
    });
  }

  private createSubBranchLinks(subGroups: Map<string, ReleaseNode[]>[], masterNodes: ReleaseNode[]): ReleaseLink[] {
    const links: ReleaseLink[] = [];
    for (const subGroup of subGroups) {
      const [branchName, subNodes] = [...subGroup.entries()][0];
      if (subNodes.length === 0) continue;

      const anchorLink = this.createAnchorLink(branchName, subNodes[0], masterNodes);
      if (anchorLink) {
        links.push(anchorLink);
      }
      links.push(...this.createIntraBranchLinks(subNodes));
    }
    return links;
  }

  private createAnchorLink(
    branchName: string,
    firstSubNode: ReleaseNode,
    masterNodes: ReleaseNode[],
  ): ReleaseLink | null {
    const miniNode = masterNodes.find((node) => node.originalBranch === branchName && node.isMiniNode);
    return miniNode ? this.buildLink(miniNode, firstSubNode) : null;
  }

  private createIntraBranchLinks(nodes: ReleaseNode[]): ReleaseLink[] {
    const links: ReleaseLink[] = [];
    for (let index = 0; index < nodes.length - 1; index++) {
      const source = nodes[index];
      const target = nodes[index + 1];

      if (!this.isVersionGap(source, target)) {
        links.push(this.buildLink(source, target));
      }
    }
    return links;
  }

  private createSpecialLinks(masterNodes: ReleaseNode[], skipNodes: SkipNode[]): ReleaseLink[] {
    if (masterNodes.length === 0) return [];

    const links: ReleaseLink[] = [];
    const firstNode = masterNodes[0];

    const initialSkipNode = skipNodes.find((s) => s.id.startsWith('skip-initial-'));

    if (initialSkipNode) {
      links.push({
        id: `fade-in-to-skip`,
        source: `start-node-${firstNode.id}`,
        target: initialSkipNode.id,
        isGap: true,
      });
    } else {
      links.push({
        id: `fade-in-link`,
        source: `start-node-${firstNode.id}`,
        target: firstNode.id,
        isGap: true,
      });
    }

    return links;
  }

  private isVersionGap(source: ReleaseNode, target: ReleaseNode): boolean {
    if (source.isMiniNode || target.isMiniNode) {
      return false;
    }

    const vSource = this.nodeService.getVersionInfo(source);
    const vTarget = this.nodeService.getVersionInfo(target);

    if (vSource && vTarget) {
      const majorGap = vTarget.major > vSource.major + 1;
      const minorGap = vSource.major === vTarget.major && vTarget.minor > vSource.minor + 1;
      return majorGap || minorGap;
    }
    return false;
  }

  private buildLink(source: ReleaseNode, target: ReleaseNode): ReleaseLink {
    return {
      id: `${source.id}-${target.id}`,
      source: source.id,
      target: target.id,
    };
  }

  private handleInitialSkipNode(skipNode: SkipNode, masterNodes: ReleaseNode[], links: ReleaseLink[]): void {
    const firstNode = masterNodes[0];
    if (firstNode) {
      links.push({
        id: `${skipNode.id}-to-${firstNode.id}`,
        source: skipNode.id,
        target: firstNode.id,
        isGap: true,
      });
    }
  }

  private handleRegularSkipNode(skipNode: SkipNode, masterNodes: ReleaseNode[], links: ReleaseLink[]): void {
    const sourceNodeIndex = this.findSourceNodeIndex(skipNode, masterNodes);

    if (sourceNodeIndex !== -1 && sourceNodeIndex < masterNodes.length - 1) {
      const sourceNode = masterNodes[sourceNodeIndex];
      const targetNode = masterNodes[sourceNodeIndex + 1];

      links.push(
        {
          id: `${sourceNode.id}-to-${skipNode.id}`,
          source: sourceNode.id,
          target: skipNode.id,
          isGap: true,
        },
        {
          id: `${skipNode.id}-to-${targetNode.id}`,
          source: skipNode.id,
          target: targetNode.id,
          isGap: true,
        },
      );
    }
  }

  private findSourceNodeIndex(skipNode: SkipNode, masterNodes: ReleaseNode[]): number {
    return masterNodes.findIndex((_, index) => {
      const nextNode = masterNodes[index + 1];
      return (
        nextNode &&
        this.isVersionGap(masterNodes[index], nextNode) &&
        skipNode.x === (masterNodes[index].position.x + nextNode.position.x) / 2
      );
    });
  }
}
