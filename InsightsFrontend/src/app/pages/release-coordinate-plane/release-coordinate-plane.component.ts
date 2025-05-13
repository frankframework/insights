import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Release, ReleaseService } from '../../services/release.service';
import { catchError, map, of, tap } from 'rxjs';

interface Position {
	x: number;
	y: number;
}

interface ReleaseNode {
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

interface ReleaseLink {
	id: string;
	source: string;
	target: string;
}

@Component({
	selector: 'app-release-coordinate-plane',
	templateUrl: './release-coordinate-plane.component.html',
	styleUrls: ['./release-coordinate-plane.component.scss']
})
export class ReleaseCoordinatePlaneComponent implements OnInit {
	@ViewChild('svgElement') svgElement!: ElementRef<SVGElement>;

	public releaseNodes: ReleaseNode[] = [];
	public releaseLinks: ReleaseLink[] = [];
	public scale = 1;
	public translateX = 0;
	public translateY = 0;
	public viewBox = '0 0 0 0';

	private isPanning = false;
	private startPan = { x: 0, y: 0 };

	private static readonly GITHUB_MASTER_BRANCH = 'master';
	private static readonly GITHUB_NIGHTLY_RELEASE = 'nightly';

	constructor(private releaseService: ReleaseService) {}

	ngOnInit(): void {
		this.getAllReleases();
	}

	private getAllReleases(): void {
		this.releaseService.getAllReleases().pipe(
				map(record => Object.values(record).flat()),
				map(releases => this.sortReleases(releases)),
				tap(sortedGroups => {
					const calculatedReleaseNodes = this.calculateReleaseCoordinates(sortedGroups);
					this.releaseNodes = this.assignReleaseColors(calculatedReleaseNodes);
					this.releaseLinks = this.createLinks(sortedGroups);

					setTimeout(() => this.centerGraph(), 0);
				}),
				catchError(err => {
					console.error('Failed to load releases:', err);
					return of([]);
				})
		).subscribe();
	}

	private sortReleases(releases: Release[]): Map<string, ReleaseNode[]>[] {
		const grouped = new Map<string, Release[]>();

		for (const release of releases) {
			const branch = release.branch.name;
			if (!grouped.has(branch)) grouped.set(branch, []);
			grouped.get(branch)!.push(release);
		}

		grouped.forEach(group => {
			group.sort((a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime());
		});

		const masterBranch = ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH;
		const masterReleases: ReleaseNode[] = (grouped.get(masterBranch) ?? []).map(r => ({
			id: r.id,
			label: r.name,
			branch: r.branch.name,
			publishedAt: new Date(r.publishedAt),
			color: '',
			position: { x: 0, y: 0 }
		}));

		grouped.delete(masterBranch);

		const otherGroups = [...grouped.entries()].map(([branch, group]) => {
			const releaseNodes: ReleaseNode[] = group.map(r => ({
				id: r.id,
				label: r.name,
				branch: r.branch.name,
				publishedAt: new Date(r.publishedAt),
				color: '',
				position: { x: 0, y: 0 }
			}));

			const firstRealSub = releaseNodes[0];
			const versionMatch = firstRealSub.label.match(/^v?(\d+)\.(\d+)/);
			if (versionMatch) {
				const syntheticLabel = `v${versionMatch[1]}.${versionMatch[2]}`;
				const syntheticNode: SyntheticNode = {
					id: `synthetic-${branch}-${firstRealSub.id}`,
					label: syntheticLabel,
					position: { x: 0, y: 0 },
					color: 'gray',
					branch: masterBranch,
					publishedAt: new Date(firstRealSub.publishedAt),
					isSynthetic: true
				};

				const insertIdx = masterReleases.findIndex(m => m.publishedAt > syntheticNode.publishedAt);
				if (insertIdx === -1) {
					masterReleases.push(syntheticNode);
				} else {
					masterReleases.splice(insertIdx, 0, syntheticNode);
				}
			}

			return new Map([[branch, releaseNodes]]);
		});

		masterReleases.sort((a, b) => a.publishedAt.getTime() - b.publishedAt.getTime());

		return [
			new Map([[masterBranch, masterReleases]]),
			...otherGroups
		];
	}

	public calculateReleaseCoordinates(sortedGroups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
		const positionedNodes = new Map<string, ReleaseNode[]>();

		const nodeMap = new Map<string, ReleaseNode[]>();
		sortedGroups.forEach(groupMap => {
			for (const [branch, nodes] of groupMap.entries()) {
				nodeMap.set(branch, nodes);
			}
		});

		const masterBranch = ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH;
		const masterNodes = nodeMap.get(masterBranch) ?? [];

		const xSpacing = 125
		const ySpacing = 75;

		masterNodes.forEach((node, idx) => {
			node.position = { x: idx * xSpacing, y: 0 };
		});

		positionedNodes.set(masterBranch, masterNodes);

		const subBranches = [...nodeMap.entries()]
				.filter(([branch]) => branch !== masterBranch)
				.sort(([, nodesA], [, nodesB]) =>
						nodesB[0].publishedAt.getTime() - nodesA[0].publishedAt.getTime()
				);

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
			const baseY = (branchIdx + 1) * ySpacing;

			const positionedSub = subNodes.map((n, i) => ({
				...n,
				position: { x: baseX + i * xSpacing, y: baseY }
			}));

			positionedNodes.set(branch, positionedSub);
		});

		return positionedNodes;
	}

	public assignReleaseColors(releaseGroups: Map<string, ReleaseNode[]>): ReleaseNode[] {
		const now = new Date();
		const allNodes: ReleaseNode[] = [];

		for (const nodes of releaseGroups.values()) {
			nodes.forEach(release => {
				const published = new Date(release.publishedAt);
				const label = release.label.toLowerCase();
				const isNightly = label.includes(ReleaseCoordinatePlaneComponent.GITHUB_NIGHTLY_RELEASE);

				if (isNightly) {
					release.color = 'darkblue';
				} else {
					const versionMatch = release.label.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
					let isMajor = true;

					if (versionMatch) {
						const patch = parseInt(versionMatch[3] ?? '0', 10);
						isMajor = patch === 0 && !/rc|beta/i.test(label);
					} else {
						isMajor = false;
					}

					const fullSupportMonths = isMajor ? 12 : 3;
					const securitySupportMonths = isMajor ? 24 : 6;

					const fullSupportEnd = new Date(published);
					fullSupportEnd.setMonth(published.getMonth() + fullSupportMonths);

					const securitySupportEnd = new Date(published);
					securitySupportEnd.setMonth(published.getMonth() + securitySupportMonths);

					release.color = now <= fullSupportEnd
							? '#30A102'
							: now <= securitySupportEnd
									? '#EF9302'
									: '#FD230E';
				}

				allNodes.push(release);
			});
		}

		return allNodes;
	}

	private createLinks(sortedGroups: Map<string, ReleaseNode[]>[]): ReleaseLink[] {
		const links: ReleaseLink[] = [];

		const masterBranch = ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH;
		const masterNodes = sortedGroups[0].get(masterBranch) ?? [];

		for (let i = 0; i < masterNodes.length - 1; i++) {
			links.push({
				id: `${masterNodes[i].id}-${masterNodes[i + 1].id}`,
				source: masterNodes[i].id,
				target: masterNodes[i + 1].id
			});
		}

		const subGroups = sortedGroups.slice(1)
				.sort((a, b) => {
					const [, aNodes] = [...a.entries()][0];
					const [, bNodes] = [...b.entries()][0];
					return bNodes[0].publishedAt.getTime() - aNodes[0].publishedAt.getTime();
				});

		for (const subGroup of subGroups) {
			const [branch, subNodes] = [...subGroup.entries()][0];
			if (!subNodes.length) continue;

			const firstSub = subNodes[0];
			const versionMatch = firstSub.label.match(/^v?(\d+)\.(\d+)/);
			if (!versionMatch) continue;

			const syntheticId = `synthetic-${branch}-${firstSub.id}`;
			const syntheticNode = masterNodes.find(n => n.id === syntheticId);

			if (syntheticNode) {
				links.push({
					id: `${syntheticNode.id}-${firstSub.id}`,
					source: syntheticNode.id,
					target: firstSub.id
				});
			}

			for (let j = 0; j < subNodes.length - 1; j++) {
				links.push({
					id: `${subNodes[j].id}-${subNodes[j + 1].id}`,
					source: subNodes[j].id,
					target: subNodes[j + 1].id
				});
			}
		}

		return links;
	}


	public getCustomPath(link: ReleaseLink): string {
		const source = this.releaseNodes.find(n => n.id === link.source);
		const target = this.releaseNodes.find(n => n.id === link.target);
		if (!source || !target) return '';

		const radius = 25;
		const [x1, y1] = [source.position.x, source.position.y];
		const [x2, y2] = [target.position.x, target.position.y];

		if (y1 === y2) {
			return `M ${x1 + radius},${y1} L ${x2 - radius},${y2}`;
		}

		const verticalDirection = y2 > y1 ? 1 : -1;
		const cornerY = y2 - verticalDirection * radius;
		const horizontalSweep = x2 > x1 ? 0 : 1;

		return [
			`M ${x1},${y1 + radius}`,
			`L ${x1},${cornerY}`,
			`A ${radius},${radius} 0 0,${horizontalSweep} ${x1 + (horizontalSweep ? -radius : radius)},${y2}`,
			`L ${x2 - radius},${y2}`
		].join(' ');
	}

	public onWheel(event: WheelEvent): void {
		event.preventDefault();

		const rect = this.svgElement.nativeElement.getBoundingClientRect();
		const mouseX = event.clientX - rect.left;
		const mouseY = event.clientY - rect.top;
		const svgX = (mouseX - this.translateX) / this.scale;
		const svgY = (mouseY - this.translateY) / this.scale;

		const delta = event.deltaY < 0 ? 1.1 : 1 / 1.1;
		this.scale *= delta;
		this.translateX = mouseX - svgX * this.scale;
		this.translateY = mouseY - svgY * this.scale;
	}

	public onMouseDown(event: MouseEvent): void {
		this.isPanning = true;
		this.startPan = { x: event.clientX, y: event.clientY };
	}

	public onMouseMove(event: MouseEvent): void {
		if (!this.isPanning) return;

		const dx = event.clientX - this.startPan.x;
		const dy = event.clientY - this.startPan.y;
		this.translateX += dx;
		this.translateY += dy;
		this.startPan = { x: event.clientX, y: event.clientY };
	}

	public onMouseUp(): void {
		this.isPanning = false;
	}

	public zoomIn(): void {
		this.applyZoom(1.1);
	}

	public zoomOut(): void {
		this.applyZoom(1 / 1.1);
	}

	private applyZoom(factor: number): void {
		const rect = this.svgElement.nativeElement.getBoundingClientRect();
		const centerX = rect.width / 2;
		const centerY = rect.height / 2;

		const svgX = (centerX - this.translateX) / this.scale;
		const svgY = (centerY - this.translateY) / this.scale;

		this.scale *= factor;
		this.translateX = centerX - svgX * this.scale;
		this.translateY = centerY - svgY * this.scale;
	}

	private centerGraph(): void {
		if (!this.svgElement || !this.releaseNodes.length) return;

		const xs = this.releaseNodes.map(n => n.position.x);
		const ys = this.releaseNodes.map(n => n.position.y);
		const minX = Math.min(...xs);
		const maxX = Math.max(...xs);
		const minY = Math.min(...ys);
		const maxY = Math.max(...ys);

		const padding = 100;
		const width = maxX - minX + padding * 2;
		const height = maxY - minY + padding * 2;
		const x = minX - padding;
		const y = minY - padding;

		this.viewBox = `${x} ${y} ${width} ${height}`;
	}
}
