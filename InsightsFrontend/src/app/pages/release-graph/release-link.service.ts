import { Injectable } from '@angular/core';
import { ReleaseNode } from './release-node.service';

export interface ReleaseLink {
	id: string;
	source: string;
	target: string;
}

@Injectable({ providedIn: 'root' })
export class ReleaseLinkService {
	private static readonly GITHUB_MASTER_BRANCH = 'master';

	createLinks(sortedGroups: Map<string, ReleaseNode[]>[]): ReleaseLink[] {
		if (sortedGroups.length === 0) return [];

		const masterBranch = ReleaseLinkService.GITHUB_MASTER_BRANCH;
		const masterNodes = sortedGroups[0].get(masterBranch) ?? [];

		return [
			...this.createReleaseNodeLinks(masterNodes),
			...this.createSubBranchLinks(sortedGroups.slice(1), masterNodes),
		];
	}

	private createSubBranchLinks(subGroups: Map<string, ReleaseNode[]>[], masterNodes: ReleaseNode[]): ReleaseLink[] {
		const sortedSubGroups = this.sortSubGroupsByLatestRelease(subGroups);
		const links: ReleaseLink[] = [];

		for (const subGroup of sortedSubGroups) {
			const [branch, subNodes] = [...subGroup.entries()][0];
			if (subNodes.length === 0) continue;

			const syntheticLink = this.createSyntheticLinkIfExists(branch, subNodes[0], masterNodes);
			if (syntheticLink) {
				links.push(syntheticLink);
			}

			links.push(...this.createReleaseNodeLinks(subNodes));
		}

		return links;
	}

	private sortSubGroupsByLatestRelease(groups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]>[] {
		return groups.sort((a, b) => {
			const [, aNodes] = [...a.entries()][0];
			const [, bNodes] = [...b.entries()][0];
			return bNodes[0].publishedAt.getTime() - aNodes[0].publishedAt.getTime();
		});
	}

	private createSyntheticLinkIfExists(
		branch: string,
		firstNode: ReleaseNode,
		masterNodes: ReleaseNode[],
	): ReleaseLink | null {
		const versionMatch = firstNode.label.match(/^v?(\d+)\.(\d+)/);
		if (!versionMatch) return null;

		const syntheticId = `synthetic-${branch}-${firstNode.id}`;
		const syntheticNode = masterNodes.find((n) => n.id === syntheticId);

		if (!syntheticNode) return null;

		return {
			id: `${syntheticNode.id}-${firstNode.id}`,
			source: syntheticNode.id,
			target: firstNode.id,
		};
	}

	private createReleaseNodeLinks(nodes: ReleaseNode[]): ReleaseLink[] {
		const links: ReleaseLink[] = [];
		for (let index = 0; index < nodes.length - 1; index++) {
			links.push(this.buildLink(nodes[index], nodes[index + 1]));
		}
		return links;
	}

	private buildLink(source: ReleaseNode, target: ReleaseNode): ReleaseLink {
		return {
			id: `${source.id}-${target.id}`,
			source: source.id,
			target: target.id,
		};
	}
}
