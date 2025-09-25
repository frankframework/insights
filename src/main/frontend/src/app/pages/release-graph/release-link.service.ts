import { Injectable, inject } from '@angular/core';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';

export interface ReleaseLink {
  id: string;
  source: string;
  target: string;
  isGap?: boolean; // Flag for styling gap links
  isFadeIn?: boolean; // Flag for the initial animation
}

export interface SkipNode {
  id: string;
  x: number;
  y: number;
  skippedCount: number;
  skippedVersions: string[]; // The actual version numbers that were skipped
  label: string; // e.g., "3 skipped"
}

@Injectable({ providedIn: 'root' })
export class ReleaseLinkService {
  private static readonly GITHUB_MASTER_BRANCH: string = 'master';
  private nodeService = inject(ReleaseNodeService);

  public createLinks(structuredGroups: Map<string, ReleaseNode[]>[]): ReleaseLink[] {
    if (structuredGroups.length === 0) return [];

    const masterNodes = structuredGroups[0].get(ReleaseLinkService.GITHUB_MASTER_BRANCH) ?? [];
    if (masterNodes.length === 0) return [];

    // The functions now generate a single, non-overlapping set of links.
    return [
      ...this.createIntraBranchLinks(masterNodes), // Now skips gaps
      ...this.createSubBranchLinks(structuredGroups.slice(1), masterNodes),
      ...this.createSpecialLinks(masterNodes), // Now fills the gaps
    ];
  }

  public createSkipNodes(structuredGroups: Map<string, ReleaseNode[]>[], allReleases: any[]): SkipNode[] {
    if (structuredGroups.length === 0) return [];

    const masterNodes = structuredGroups[0].get(ReleaseLinkService.GITHUB_MASTER_BRANCH) ?? [];
    if (masterNodes.length === 0) return [];

    const skipNodes: SkipNode[] = [];
    const allNodes = [...structuredGroups.values()].flat().flatMap(nodes => [...nodes.values()]).flat();

    // Create skip nodes for version gaps between existing nodes
    for (let i = 0; i < masterNodes.length - 1; i++) {
      const source = masterNodes[i];
      const target = masterNodes[i + 1];

      if (this.isVersionGap(source, target)) {
        const skippedReleases = this.findSkippedReleasesBetweenNodes(source, target, allNodes, allReleases);
        const minorReleases = skippedReleases.filter(r => {
          const info = this.nodeService.getVersionInfo({ label: r.name } as ReleaseNode);
          return info?.type === 'minor';
        });

        if (minorReleases.length > 0) {
          skipNodes.push({
            id: `skip-${source.id}-${target.id}`,
            x: (source.position.x + target.position.x) / 2,
            y: source.position.y,
            skippedCount: minorReleases.length,
            skippedVersions: skippedReleases.map(r => r.name.startsWith('v') ? r.name : `v${r.name}`),
            label: minorReleases.length === 1 ? '1 skipped' : `${minorReleases.length} skipped`
          });
        }
      }
    }

    // Check for initial gap before first release
    if (masterNodes.length > 0) {
      const firstNode = masterNodes[0];
      const skippedReleases = this.findSkippedReleasesBeforeNode(firstNode, allNodes, allReleases);
      const minorReleases = skippedReleases.filter(r => {
        const info = this.nodeService.getVersionInfo({ label: r.name } as ReleaseNode);
        return info?.type === 'minor';
      });

      if (minorReleases.length > 0) {
        skipNodes.push({
          id: `skip-initial-${firstNode.id}`,
          x: firstNode.position.x - 175,
          y: firstNode.position.y,
          skippedCount: minorReleases.length,
          skippedVersions: skippedReleases.map(r => r.name.startsWith('v') ? r.name : `v${r.name}`),
          label: minorReleases.length === 1 ? '1 skipped' : `${minorReleases.length} skipped`
        });
      }
    }

    return skipNodes;
  }

  private findSkippedReleasesBetweenNodes(source: ReleaseNode, target: ReleaseNode, allNodes: ReleaseNode[], allReleases: any[]): any[] {
    const vSource = this.nodeService.getVersionInfo(source);
    const vTarget = this.nodeService.getVersionInfo(target);
    if (!vSource || !vTarget) return [];

    return allReleases.filter(release => {
      const vRelease = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!vRelease) return false;

      const releaseInGraph = allNodes.some(node => node.id === release.id);
      if (releaseInGraph) return false;

      if (vSource.major === vTarget.major) {
        return vRelease.major === vSource.major &&
               vRelease.minor > vSource.minor &&
               vRelease.minor < vTarget.minor;
      } else {
        return (vRelease.major > vSource.major && vRelease.major < vTarget.major) ||
               (vRelease.major === vSource.major && vRelease.minor > vSource.minor) ||
               (vRelease.major === vTarget.major && vRelease.minor < vTarget.minor);
      }
    });
  }

  private findSkippedReleasesBeforeNode(firstNode: ReleaseNode, allNodes: ReleaseNode[], allReleases: any[]): any[] {
    const vFirst = this.nodeService.getVersionInfo(firstNode);
    if (!vFirst) return [];

    return allReleases.filter(release => {
      const vRelease = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!vRelease) return false;

      const releaseInGraph = allNodes.some(node => node.id === release.id);
      if (releaseInGraph) return false;

      return (vRelease.major < vFirst.major) ||
             (vRelease.major === vFirst.major && vRelease.minor < vFirst.minor);
    });
  }

  public createSkipNodeLinks(skipNodes: SkipNode[], masterNodes: ReleaseNode[]): ReleaseLink[] {
    const links: ReleaseLink[] = [];

    for (const skipNode of skipNodes) {
      // Handle initial skip node (before first release)
      if (skipNode.id.startsWith('skip-initial-')) {
        const firstNode = masterNodes[0];
        if (firstNode) {
          // Create dotted link from fade-in to skip node
          links.push({
            id: `fade-in-to-${skipNode.id}`,
            source: `start-node-${firstNode.id}`,
            target: skipNode.id,
            isGap: true
          });

          // Create dotted link from skip node to first actual node
          links.push({
            id: `${skipNode.id}-to-${firstNode.id}`,
            source: skipNode.id,
            target: firstNode.id,
            isGap: true
          });
        }
      } else {
        // Handle regular gap skip nodes
        const sourceNodeIndex = masterNodes.findIndex((_, index) => {
          const nextNode = masterNodes[index + 1];
          return nextNode && this.isVersionGap(masterNodes[index], nextNode) &&
                 skipNode.x === (masterNodes[index].position.x + nextNode.position.x) / 2;
        });

        if (sourceNodeIndex >= 0 && sourceNodeIndex < masterNodes.length - 1) {
          const sourceNode = masterNodes[sourceNodeIndex];
          const targetNode = masterNodes[sourceNodeIndex + 1];

          // Create dotted link from source to skip node
          links.push({
            id: `${sourceNode.id}-to-${skipNode.id}`,
            source: sourceNode.id,
            target: skipNode.id,
            isGap: true
          });

          // Create dotted link from skip node to target
          links.push({
            id: `${skipNode.id}-to-${targetNode.id}`,
            source: skipNode.id,
            target: targetNode.id,
            isGap: true
          });
        }
      }
    }

    return links;
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
    const anchorNodeOnMaster = masterNodes.find((node) => node.originalBranch === branchName);
    return anchorNodeOnMaster ? this.buildLink(anchorNodeOnMaster, firstSubNode) : null;
  }

  /**
   * Creates sequential links but now intelligently SKIPS creating a link if there's a version gap.
   */
  private createIntraBranchLinks(nodes: ReleaseNode[]): ReleaseLink[] {
    const links: ReleaseLink[] = [];
    for (let i = 0; i < nodes.length - 1; i++) {
      const source = nodes[i];
      const target = nodes[i + 1];

      // Only build a solid link if it's NOT a gap.
      if (!this.isVersionGap(source, target)) {
        links.push(this.buildLink(source, target));
      }
    }
    return links;
  }

  /**
   * Creates the fade-in line at the start. Skip nodes may override this.
   */
  private createSpecialLinks(masterNodes: ReleaseNode[]): ReleaseLink[] {
    if (masterNodes.length === 0) return [];

    const links: ReleaseLink[] = [];
    const firstNode = masterNodes[0];

    // Create the initial fade-in link (this may be overridden by skip node links)
    links.push({
      id: `fade-in-link`,
      source: `start-node-${firstNode.id}`,
      target: firstNode.id,
      isFadeIn: true,
    });

    return links;
  }

  private isVersionGap(source: ReleaseNode, target: ReleaseNode): boolean {
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
}
