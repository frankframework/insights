import { Injectable, inject } from '@angular/core';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';

export interface ReleaseLink {
  id: string;
  source: string;
  target: string;
  isGap?: boolean; // Flag for styling gap links
  isFadeIn?: boolean; // Flag for the initial animation
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
   * Creates dotted lines for gaps and a fade-in line at the start.
   */
  private createSpecialLinks(masterNodes: ReleaseNode[]): ReleaseLink[] {
    if (masterNodes.length === 0) return [];

    const links: ReleaseLink[] = [];
    const firstNode = masterNodes[0];

    // 1. Create the initial fade-in link
    links.push({
      id: `fade-in-link`,
      source: `start-node-${firstNode.id}`,
      target: firstNode.id,
      isFadeIn: true,
    });

    // 2. Create gap links where detected
    for (let i = 0; i < masterNodes.length - 1; i++) {
      const source = masterNodes[i];
      const target = masterNodes[i + 1];

      if (this.isVersionGap(source, target)) {
        links.push({ ...this.buildLink(source, target), isGap: true });
      }
    }
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
