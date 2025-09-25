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

  public createSkipNodes(structuredGroups: Map<string, ReleaseNode[]>[]): SkipNode[] {
    if (structuredGroups.length === 0) return [];

    const masterNodes = structuredGroups[0].get(ReleaseLinkService.GITHUB_MASTER_BRANCH) ?? [];
    if (masterNodes.length === 0) return [];

    const skipNodes: SkipNode[] = [];

    // Check for initial gap before first release
    if (masterNodes.length > 0) {
      const firstNode = masterNodes[0];
      const vFirst = this.nodeService.getVersionInfo(firstNode);

      if (vFirst && (vFirst.major > 1 || (vFirst.major === 1 && vFirst.minor > 0))) {
        const skippedVersions: string[] = [];
        let skippedCount = 0;

        if (vFirst.major > 1) {
          // Major versions before first major
          for (let major = 1; major < vFirst.major; major++) {
            skippedVersions.push(`v${major}.0`);
            skippedCount++;
          }
        }

        if (vFirst.major >= 1 && vFirst.minor > 0) {
          // Minor versions before first minor in the same major
          for (let minor = 0; minor < vFirst.minor; minor++) {
            skippedVersions.push(`v${vFirst.major}.${minor}`);
            skippedCount++;
          }
        }

        if (skippedCount > 0) {
          // Position before the fade-in line
          skipNodes.push({
            id: `skip-initial-${firstNode.id}`,
            x: firstNode.position.x - 175, // Between fade-in start and first node
            y: firstNode.position.y,
            skippedCount,
            skippedVersions,
            label: skippedCount === 1 ? '1 skipped' : `${skippedCount} skipped`
          });
        }
      }
    }

    // Create skip nodes for version gaps between existing nodes
    for (let i = 0; i < masterNodes.length - 1; i++) {
      const source = masterNodes[i];
      const target = masterNodes[i + 1];

      if (this.isVersionGap(source, target)) {
        const vSource = this.nodeService.getVersionInfo(source);
        const vTarget = this.nodeService.getVersionInfo(target);

        if (vSource && vTarget) {
          const skippedVersions: string[] = [];
          let skippedCount = 0;

          if (vTarget.major > vSource.major + 1) {
            // Major version gaps
            for (let major = vSource.major + 1; major < vTarget.major; major++) {
              skippedVersions.push(`v${major}.0`);
              skippedCount++;
            }
          } else if (vSource.major === vTarget.major && vTarget.minor > vSource.minor + 1) {
            // Minor version gaps
            for (let minor = vSource.minor + 1; minor < vTarget.minor; minor++) {
              skippedVersions.push(`v${vSource.major}.${minor}`);
              skippedCount++;
            }
          }

          if (skippedCount > 0) {
            skipNodes.push({
              id: `skip-${source.id}-${target.id}`,
              x: (source.position.x + target.position.x) / 2,
              y: source.position.y,
              skippedCount,
              skippedVersions,
              label: skippedCount === 1 ? '1 skipped' : `${skippedCount} skipped`
            });
          }
        }
      }
    }

    return skipNodes;
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
