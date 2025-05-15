import { Injectable } from '@angular/core';
import {Release} from "../../services/release.service";
import {ReleaseGraphComponent} from "./release-graph.component";

interface Position {
	x: number;
	y: number;
}

export interface ReleaseNode {
	id: string;
	label: string;
	position: Position;
	color: string;
	branch: string;
	publishedAt: Date;
}

interface SyntheticNode extends ReleaseNode {
	isSynthetic?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ReleaseNodeService {
	private static readonly GITHUB_NIGHTLY_RELEASE = 'nightly';

	public sortReleases(releases: Release[]): Map<string, ReleaseNode[]>[] {
		const grouped = this.groupReleasesByBranch(releases);
		this.sortGroupedReleases(grouped);

		const masterBranch = ReleaseGraphComponent.GITHUB_MASTER_BRANCH;
		const masterReleases = this.createReleaseNodes(grouped.get(masterBranch) ?? []);

		grouped.delete(masterBranch);

		const otherGroups = this.createOtherReleaseGroups(grouped, masterReleases, masterBranch);

		this.sortByDate(masterReleases);

		return [
			new Map([[masterBranch, masterReleases]]),
			...otherGroups
		];
	}

	public calculateReleaseCoordinates(sortedGroups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
		const nodeMap = this.flattenGroupMaps(sortedGroups);

		const masterBranch = ReleaseGraphComponent.GITHUB_MASTER_BRANCH;
		const masterNodes = nodeMap.get(masterBranch) ?? [];
		this.positionMasterNodes(masterNodes);

		const positionedNodes = new Map<string, ReleaseNode[]>();
		positionedNodes.set(masterBranch, masterNodes);

		const subBranches = this.getSortedSubBranches(nodeMap, masterBranch);
		this.positionSubBranches(subBranches, masterNodes, positionedNodes);

		return positionedNodes;
	}

	public assignReleaseColors(releaseGroups: Map<string, ReleaseNode[]>): ReleaseNode[] {
		const now = new Date();
		const allNodes: ReleaseNode[] = [];

		for (const nodes of releaseGroups.values()) {
			nodes.forEach(release => {
				release.color = this.determineColor(release, now);
				allNodes.push(release);
			});
		}

		return allNodes;
	}

	private groupReleasesByBranch(releases: Release[]): Map<string, Release[]> {
		const grouped = new Map<string, Release[]>();
		for (const release of releases) {
			const branch = release.branch.name;
			if (!grouped.has(branch)) grouped.set(branch, []);
			grouped.get(branch)!.push(release);
		}
		return grouped;
	}

	private sortGroupedReleases(grouped: Map<string, Release[]>) {
		grouped.forEach(group => {
			group.sort((a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime());
		});
	}

	private createReleaseNodes(releases: Release[]): ReleaseNode[] {
		return releases.map(r => ({
			id: r.id,
			label: r.name,
			branch: r.branch.name,
			publishedAt: new Date(r.publishedAt),
			color: '',
			position: { x: 0, y: 0 }
		}));
	}

	private createOtherReleaseGroups(grouped: Map<string, Release[]>, masterReleases: ReleaseNode[], masterBranch: string): Map<string, ReleaseNode[]>[] {
		return [...grouped.entries()].map(([branch, group]) => {
			const releaseNodes = this.createReleaseNodes(group);

			const syntheticNode = this.createSyntheticNodeIfNeeded(releaseNodes[0], branch, masterBranch);
			if (syntheticNode) {
				this.insertSyntheticNode(masterReleases, syntheticNode);
			}

			return new Map([[branch, releaseNodes]]);
		});
	}

	private createSyntheticNodeIfNeeded(firstNode: ReleaseNode, branch: string, masterBranch: string): SyntheticNode | null {
		const versionMatch = firstNode.label.match(/^v?(\d+)\.(\d+)/);
		if (!versionMatch) return null;

		return {
			id: `synthetic-${branch}-${firstNode.id}`,
			label: `v${versionMatch[1]}.${versionMatch[2]}`,
			position: { x: 0, y: 0 },
			color: 'gray',
			branch: masterBranch,
			publishedAt: new Date(firstNode.publishedAt),
			isSynthetic: true
		};
	}

	private insertSyntheticNode(masterReleases: ReleaseNode[], syntheticNode: SyntheticNode) {
		const insertIdx = masterReleases.findIndex(m => m.publishedAt > syntheticNode.publishedAt);
		if (insertIdx === -1) {
			masterReleases.push(syntheticNode);
		} else {
			masterReleases.splice(insertIdx, 0, syntheticNode);
		}
	}

	private sortByDate(nodes: ReleaseNode[]) {
		nodes.sort((a, b) => a.publishedAt.getTime() - b.publishedAt.getTime());
	}

	private flattenGroupMaps(groupMaps: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
		const result = new Map<string, ReleaseNode[]>();
		for (const group of groupMaps) {
			for (const [branch, nodes] of group.entries()) {
				result.set(branch, nodes);
			}
		}
		return result;
	}

	private positionMasterNodes(nodes: ReleaseNode[]) {
		const masterReleaseNodesSpacing = 125;
		nodes.forEach((node, idx) => {
			node.position = { x: idx * masterReleaseNodesSpacing, y: 0 };
		});
	}

	private getSortedSubBranches(nodeMap: Map<string, ReleaseNode[]>, masterBranch: string): [string, ReleaseNode[]][] {
		return [...nodeMap.entries()]
				.filter(([branch]) => branch !== masterBranch)
				.sort(([, nodesA], [, nodesB]) =>
						nodesB[0].publishedAt.getTime() - nodesA[0].publishedAt.getTime()
				);
	}

	private positionSubBranches(
			subBranches: [string, ReleaseNode[]][],
			masterNodes: ReleaseNode[],
			positionedNodes: Map<string, ReleaseNode[]>
	) {
		const subReleaseNodeXSpacing = 125;
		const subReleaseNodeYSpacing = 75;

		subBranches.forEach(([branch, subNodes], branchIdx) => {
			if (!subNodes.length) return;

			const firstSub = subNodes[0];
			const versionMatch = firstSub.label.match(/^v?(\d+)\.(\d+)/);
			if (!versionMatch) return;

			const matchingSynthetic = masterNodes.find(n =>
					n.label === `v${versionMatch[1]}.${versionMatch[2]}` && (n as SyntheticNode).isSynthetic
			);

			if (!matchingSynthetic) return;

			const baseX = matchingSynthetic.position.x + 75;
			const baseY = (branchIdx + 1) * subReleaseNodeYSpacing;

			const positionedSub = subNodes.map((n, i) => ({
				...n,
				position: { x: baseX + i * subReleaseNodeXSpacing, y: baseY }
			}));

			positionedNodes.set(branch, positionedSub);
		});
	}

	private determineColor(release: ReleaseNode, now: Date): string {
		const label = release.label.toLowerCase();
		if (label.includes(ReleaseNodeService.GITHUB_NIGHTLY_RELEASE)) {
			return 'darkblue';
		}

		const versionMatch = release.label.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
		let isMajor: boolean;

		if (versionMatch) {
			const patch = parseInt(versionMatch[3] ?? '0', 10);
			isMajor = patch === 0 && !/rc|b/i.test(label);
		} else {
			isMajor = false;
		}

		const fullSupportMonths = isMajor ? 12 : 3;
		const securitySupportMonths = isMajor ? 24 : 6;

		const published = new Date(release.publishedAt);
		const fullSupportEnd = new Date(published);
		fullSupportEnd.setMonth(published.getMonth() + fullSupportMonths);

		const securitySupportEnd = new Date(published);
		securitySupportEnd.setMonth(published.getMonth() + securitySupportMonths);

		return now <= fullSupportEnd
				? '#30A102'
				: now <= securitySupportEnd
						? '#EF9302'
						: '#FD230E';
	}
}
