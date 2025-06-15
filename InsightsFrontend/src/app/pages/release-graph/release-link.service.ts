import { Injectable } from '@angular/core';
import { ReleaseNode } from './release-node.service';

export interface ReleaseLink {
  id: string;
  source: string;
  target: string;
}

@Injectable({ providedIn: 'root' })
export class ReleaseLinkService {
  private static readonly GITHUB_MASTER_BRANCH: string = 'master';

  /**
   * Creëert alle links voor de graaf: zowel binnen branches als tussen de master en sub-branches.
   */
  createLinks(structuredGroups: Map<string, ReleaseNode[]>[]): ReleaseLink[] {
    if (structuredGroups.length === 0) return [];

    const masterNodes = structuredGroups[0].get(ReleaseLinkService.GITHUB_MASTER_BRANCH) ?? [];

    return [
      ...this.createIntraBranchLinks(masterNodes),
      ...this.createSubBranchLinks(structuredGroups.slice(1), masterNodes),
    ];
  }

  /**
   * Creëert links voor alle sub-branches, inclusief de cruciale link van de master-branch naar de sub-branch.
   */
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

  /**
   * AANGEPAST: Deze functie vervangt de oude 'createSyntheticLinkIfExists'.
   * Het zoekt de juiste anker-node op de master-branch (die we daarheen hebben verplaatst)
   * en creëert een link naar de eerste node van de sub-branch.
   */
  private createAnchorLink(
    branchName: string,
    firstSubNode: ReleaseNode,
    masterNodes: ReleaseNode[],
  ): ReleaseLink | null {
    const anchorNodeOnMaster = masterNodes.find((node) => node.originalBranch === branchName);

    if (!anchorNodeOnMaster) {
      return null;
    }

    return this.buildLink(anchorNodeOnMaster, firstSubNode);
  }

  /**
   * Creëert de opeenvolgende links tussen nodes BINNEN een enkele branch.
   * (Voorheen 'createReleaseNodeLinks')
   */
  private createIntraBranchLinks(nodes: ReleaseNode[]): ReleaseLink[] {
    const links: ReleaseLink[] = [];
    for (let index = 0; index < nodes.length - 1; index++) {
      links.push(this.buildLink(nodes[index], nodes[index + 1]));
    }
    return links;
  }

  /**
   * Een simpele helper-functie om een link-object te bouwen.
   */
  private buildLink(source: ReleaseNode, target: ReleaseNode): ReleaseLink {
    return {
      id: `${source.id}-${target.id}`,
      source: source.id,
      target: target.id,
    };
  }
}
