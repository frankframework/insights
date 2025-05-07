import {
	Component,
	AfterViewInit,
	ViewChild,
	ElementRef,
	OnInit
} from '@angular/core';
import { Release, ReleaseService } from '../../services/release.service';
import { map, tap, catchError, of } from 'rxjs';

type Position = { x: number; y: number };

interface ReleaseNode {
	id: string;
	label: string;
	position: Position;
	color: string;
	branch: string;
	publishedAt: Date;
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
export class ReleaseCoordinatePlaneComponent implements OnInit, AfterViewInit {
	@ViewChild('svgElement') svgElement!: ElementRef<SVGElement>;

	protected viewBox = '0 0 500 500';
	public releaseNodes: ReleaseNode[] = [];
	public releaseLinks: ReleaseLink[] = [];

	private static readonly GITHUB_MASTER_BRANCH: string = 'master';


	public scale: number = 1;
	public translateX: number = 0;
	public translateY: number = 0;

	private isPanning = false;
	private startPan = { x: 0, y: 0 };


	constructor(private releaseService: ReleaseService) {}

	ngOnInit(): void {
		this.getAllReleases();
	}

	ngAfterViewInit(): void {
		setTimeout(() => this.centerGraph(), 0);
	}

	public onWheel(event: WheelEvent): void {
		event.preventDefault();

		const svg = this.svgElement.nativeElement;
		const rect = svg.getBoundingClientRect();
		const mouseX = event.clientX - rect.left;
		const mouseY = event.clientY - rect.top;

		// Convert mouse screen coords to SVG coords
		const svgX = (mouseX - this.translateX) / this.scale;
		const svgY = (mouseY - this.translateY) / this.scale;

		// Zoom in or out
		const zoomFactor = 1.1;
		const delta = event.deltaY < 0 ? zoomFactor : 1 / zoomFactor;
		this.scale *= delta;

		// Adjust translation to keep mouse point stable
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
		const svg = this.svgElement.nativeElement;
		const rect = svg.getBoundingClientRect();

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

		const minX = Math.min(...this.releaseNodes.map(n => n.position.x));
		const maxX = Math.max(...this.releaseNodes.map(n => n.position.x));
		const minY = Math.min(...this.releaseNodes.map(n => n.position.y));
		const maxY = Math.max(...this.releaseNodes.map(n => n.position.y));

		let viewBoxWidth = (maxX - minX) * 2;
		let viewBoxHeight = (maxY - minY + 200) * 2;
		let viewBoxX = minX - viewBoxWidth / 4;
		let viewBoxY = minY - viewBoxHeight / 4;

		this.svgElement.nativeElement.setAttribute(
				'viewBox',
				`${viewBoxX} ${viewBoxY} ${viewBoxWidth} ${viewBoxHeight}`
		);
	}

	private getAllReleases(): void {
		this.releaseService.getAllReleases().pipe(
				map(record => {
					return Object.values(record);
				}),
				tap(releases => {
					console.log(releases);
					if (!Array.isArray(releases)) {
						console.error('Expected releases to be an array, got:', releases);
						return;
					}
					const sortedGroups = this.sortReleases(releases);
					const nodes = this.calculateReleaseCoordinates(sortedGroups);
					this.releaseNodes = this.assignReleaseColors(nodes);
					this.releaseLinks = this.createLinks(sortedGroups);
				}),
				catchError(err => {
					console.error('Failed to load releases:', err);
					return of([]);
				})
		).subscribe();
	}

	private sortReleases(releases: Release[]): Map<string, Release[]>[] {
		const grouped = new Map<string, Release[]>();

		for (const release of releases) {
			const branch = release.branch.name;
			if (!grouped.has(branch)) {
				grouped.set(branch, []);
			}
			grouped.get(branch)!.push(release);
		}

		for (const group of grouped.values()) {
			group.sort((a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime());
		}

		const masterReleases = grouped.get(ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH) ?? [];
		grouped.delete(ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH);

		const otherGroups = [...grouped.entries()].map(([branch, group]) => new Map([[branch, group]]));

		otherGroups.sort((a, b) => {
			const aFirst = [...a.values()][0][0]?.publishedAt ?? '';
			const bFirst = [...b.values()][0][0]?.publishedAt ?? '';
			return new Date(bFirst).getTime() - new Date(aFirst).getTime();
		});

		for (const groupMap of otherGroups) {
			const group = [...groupMap.values()][0];
			const firstRelease = group[0];
			if (firstRelease) {
				masterReleases.push(firstRelease);
				group.splice(0, 1);
			}
		}

		masterReleases.sort((a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime());

		const result: Map<string, Release[]>[] = [
			new Map([[ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH, masterReleases]]),
			...otherGroups
		];

		return result;
	}

	private calculateReleaseCoordinates(sortedGroups: Map<string, Release[]>[]): ReleaseNode[] {
		let xOffset = 0;
		let nodes: ReleaseNode[] = [];
		let yStep = 100;

		const subReleaseCountMap = new Map<string, number>();

		const masterGroupMap = sortedGroups[0];
		const masterBranch = ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH;
		const masterGroup = masterGroupMap.get(masterBranch) ?? [];

		for (let i = 1; i < sortedGroups.length; i++) {
			const [_, subGroup] = [...sortedGroups[i].entries()][0];
			const firstSubRelease = subGroup[0];
			if (!firstSubRelease) continue;

			const parentMaster = masterGroup
					.filter(m => new Date(m.publishedAt) <= new Date(firstSubRelease.publishedAt))
					.reduce((latest, current) =>
									!latest || new Date(current.publishedAt) > new Date(latest.publishedAt)
											? current
											: latest,
							null as Release | null
					);

			if (parentMaster) {
				const current = subReleaseCountMap.get(parentMaster.id) ?? 0;
				subReleaseCountMap.set(parentMaster.id, current + subGroup.length);
			}
		}

		sortedGroups.forEach((groupMap, rowIndex) => {
			const [branch, group] = [...groupMap.entries()][0];
			const y = rowIndex * yStep;
			let x = xOffset;

			const groupNodes: ReleaseNode[] = group.map((release, i) => ({

				id: release.id,
				label: release.tagName,
				position: { x: x + i * 100, y },
				color: 'pink',
				branch,
				publishedAt: new Date(release.publishedAt)
			}));

			nodes.push(...groupNodes);

			if (branch === masterBranch) {
				const lastNode = groupNodes.at(-1);
				if (lastNode) {
					const subCount = subReleaseCountMap.get(lastNode.id) ?? 1;
					xOffset = lastNode.position.x + subCount * 140;
				}
			}
		});

		return nodes;
	}

	private assignReleaseColors(nodes: ReleaseNode[]): ReleaseNode[] {
		const now = new Date();
		return nodes.map(node => {
			const ageMonths = (now.getTime() - new Date(node.publishedAt).getTime()) / (1000 * 60 * 60 * 24 * 30);

			let color = 'black';
			if (node.label.toLowerCase().includes('nightly')) {
				color = 'blue';
			} else if (ageMonths > 6) {
				color = 'orange';
			}

			return { ...node, color };
		});
	}

	private createLinks(sortedGroups: Map<string, Release[]>[]): ReleaseLink[] {
		const links: ReleaseLink[] = [];
		const nodeMap = new Map<string, ReleaseNode>(this.releaseNodes.map(n => [n.id, n]));

		for (const groupMap of sortedGroups) {
			const [_, group] = [...groupMap.entries()][0];

			for (let i = 0; i < group.length - 1; i++) {
				links.push({
					id: `${group[i].id}-${group[i + 1].id}`,
					source: group[i].id,
					target: group[i + 1].id
				});
			}
		}

		const masterGroup = sortedGroups[0].get(ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH) || [];
		const masterNodes = masterGroup.map(r => nodeMap.get(r.id)!).filter(Boolean);

		for (let i = 1; i < sortedGroups.length; i++) {
			const [_, group] = [...sortedGroups[i].entries()][0];
			const firstSubRelease = nodeMap.get(group.at(0)?.id ?? '');
			if (!firstSubRelease) continue;

			const parentMaster = masterNodes
					.filter(m => m.publishedAt <= firstSubRelease.publishedAt)
					.reduce((latest, current) => {
						return !latest || current.publishedAt > latest.publishedAt ? current : latest;
					}, null as ReleaseNode | null);

			if (parentMaster) {
				links.push({
					id: `${parentMaster.id}-${firstSubRelease.id}`,
					source: parentMaster.id,
					target: firstSubRelease.id
				});
			}
		}

		return links;
	}

	public getCustomPath(link: { source: string; target: string }): string {
		const sourceNode = this.releaseNodes.find(n => n.id === link.source);
		const targetNode = this.releaseNodes.find(n => n.id === link.target);
		if (!sourceNode || !targetNode) return '';

		const { x: x1, y: y1 } = sourceNode.position;
		const { x: x2, y: y2 } = targetNode.position;

		const nodeRadius = 20;

		if (y1 === y2) {
			return `M ${x1 + nodeRadius},${y1} L ${x2 - nodeRadius},${y2}`;
		}

		const startX = x1;
		const startY = y1 + nodeRadius;

		const endX = x2 - nodeRadius;
		const endY = y2;

		const arcRadius = Math.abs(x2 - x1);
		const sweepFlag = x2 < x1 ? 1 : 0;

		return `
		M ${startX},${startY}
		A ${arcRadius},${arcRadius} 0 0,${sweepFlag} ${endX},${endY}
	`.trim();
	}

}
