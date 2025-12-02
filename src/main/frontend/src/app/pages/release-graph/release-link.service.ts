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
  sourceNodeId?: string;
  targetNodeId?: string;
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

    const allNodes = this.getAllNodesFlat(structuredGroups);

    return [
      ...this.createIntraBranchLinks(masterNodes, allNodes),
      ...this.createBranchLinks(structuredGroups.slice(1), masterNodes),
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

      if (this.isVersionGap(source, target, allNodes)) {
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
      sourceNodeId: source.id,
      targetNodeId: target.id,
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
    const sourceNode = source.isMiniNode ? this.getLinkedBranchNode(source, allNodes) : source;
    const targetNode = target.isMiniNode ? this.getLinkedBranchNode(target, allNodes) : target;

    if (!sourceNode || !targetNode) return [];

    const vSource = this.nodeService.getVersionInfo(sourceNode);
    const vTarget = this.nodeService.getVersionInfo(targetNode);
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
    const nodeToCheck = firstNode.isMiniNode ? this.getLinkedBranchNode(firstNode, allNodes) : firstNode;
    if (!nodeToCheck) return [];

    const vFirst = this.nodeService.getVersionInfo(nodeToCheck);
    if (!vFirst) return [];

    return allReleases.filter((release) => {
      const vRelease = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!vRelease) return false;

      const releaseInGraph = allNodes.some((node) => node.id === release.id);
      if (releaseInGraph) return false;

      return vRelease.major < vFirst.major || (vRelease.major === vFirst.major && vRelease.minor < vFirst.minor);
    });
  }

  private createBranchLinks(branchGroups: Map<string, ReleaseNode[]>[], masterNodes: ReleaseNode[]): ReleaseLink[] {
    const links: ReleaseLink[] = [];
    for (const branchGroup of branchGroups) {
      const [branchName, branchNodes] = [...branchGroup.entries()][0];
      if (branchNodes.length === 0) continue;

      const anchorLink = this.createAnchorLink(branchName, branchNodes[0], masterNodes);
      if (anchorLink) {
        links.push(anchorLink);
      }
      links.push(...this.createIntraBranchLinks(branchNodes));
    }
    return links;
  }

  private createAnchorLink(
    branchName: string,
    firstBranchNode: ReleaseNode,
    masterNodes: ReleaseNode[],
  ): ReleaseLink | null {
    const miniNode = masterNodes.find((node) => node.originalBranch === branchName && node.isMiniNode);
    return miniNode ? this.buildLink(miniNode, firstBranchNode) : null;
  }

  private createIntraBranchLinks(nodes: ReleaseNode[], allNodes: ReleaseNode[] = []): ReleaseLink[] {
    const links: ReleaseLink[] = [];
    for (let index = 0; index < nodes.length - 1; index++) {
      const source = nodes[index];
      const target = nodes[index + 1];

      if (!this.isVersionGap(source, target, allNodes)) {
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

  private isVersionGap(source: ReleaseNode, target: ReleaseNode, allNodes: ReleaseNode[] = []): boolean {
    const sourceNode = source.isMiniNode ? this.getLinkedBranchNode(source, allNodes) : source;
    const targetNode = target.isMiniNode ? this.getLinkedBranchNode(target, allNodes) : target;

    if (!sourceNode || !targetNode) return false;

    const vSource = this.nodeService.getVersionInfo(sourceNode);
    const vTarget = this.nodeService.getVersionInfo(targetNode);

    if (vSource && vTarget) {
      const majorTransition = vTarget.major > vSource.major;

      const minorGap = vSource.major === vTarget.major && vTarget.minor > vSource.minor + 1;

      return majorTransition || minorGap;
    }
    return false;
  }

  private getLinkedBranchNode(miniNode: ReleaseNode, allNodes: ReleaseNode[]): ReleaseNode | null {
    if (!miniNode.linkedBranchNode) return null;
    return allNodes.find((node) => node.id === miniNode.linkedBranchNode) ?? null;
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
    if (!skipNode.sourceNodeId || !skipNode.targetNodeId) return;

    const sourceNode = masterNodes.find((node) => node.id === skipNode.sourceNodeId);
    const targetNode = masterNodes.find((node) => node.id === skipNode.targetNodeId);

    if (sourceNode && targetNode) {
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
}
