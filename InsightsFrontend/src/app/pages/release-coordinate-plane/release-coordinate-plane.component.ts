import {AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Release, ReleaseService} from '../../services/release.service';
import {catchError, map, of, tap} from 'rxjs';

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

	public releaseNodes: ReleaseNode[] = [];
	public releaseLinks: ReleaseLink[] = [];
	public scale = 1;
	public translateX = 0;
	public translateY = 0;
	protected viewBox = '0 0 500 500';

	private isPanning = false;
	private startPan = { x: 0, y: 0 };

	private static readonly GITHUB_MASTER_BRANCH = 'master';
	private static readonly GITHUB_NIGHTLY_RELEASE = 'nightly';

	constructor(private releaseService: ReleaseService) {}

	ngOnInit(): void {
		this.getAllReleases();
	}

	ngAfterViewInit(): void {
		setTimeout(() => this.centerGraph(), 0);
	}

	private getAllReleases(): void {
		this.releaseService.getAllReleases().pipe(
				map(record => Object.values(record).flat()),
				map(releases => this.sortReleases(releases)),
				tap(sortedGroups => {
					const calculatedReleaseNodes = this.calculateReleaseCoordinates(sortedGroups);
					this.releaseNodes = this.assignReleaseColors(calculatedReleaseNodes);
					this.releaseLinks = this.createLinks(sortedGroups);
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
			const firstRelease = group.shift();
			if (firstRelease) masterReleases.push(firstRelease);
		}

		masterReleases.sort((a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime());

		const convertToNode = (r: Release): ReleaseNode => ({
			id: r.id,
			label: r.name,
			branch: r.branch.name,
			publishedAt: new Date(r.publishedAt),
			color: '',
			position: { x: 0, y: 0 }
		});

		return [
			new Map([[ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH, masterReleases.map(convertToNode)]]),
			...otherGroups.map(groupMap => {
				const [branch, releases] = [...groupMap.entries()][0];
				return new Map([[branch, releases.map(convertToNode)]]);
			})
		];
	}

	public calculateReleaseCoordinates(sortedGroups: Map<string, ReleaseNode[]>[]): Map<string, ReleaseNode[]> {
		const nodeMap = new Map<string, ReleaseNode[]>();
		sortedGroups.forEach(groupMap => {
			for (const [branch, nodes] of groupMap.entries()) {
				nodeMap.set(branch, nodes);
			}
		});

		const masterBranch = ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH;
		const masterNodes = nodeMap.get(masterBranch) ?? [];

		const positionedNodes = new Map<string, ReleaseNode[]>();

		const branchNames = [...nodeMap.keys()].filter(b => b !== masterBranch);
		const branchIndex = new Map<string, number>();
		branchNames.forEach((b, i) => branchIndex.set(b, i));

		const xSpacing = 150;
		const ySpacing = 75;

		masterNodes.forEach((masterNode, masterIdx) => {
			const x = masterIdx * xSpacing;
			masterNode.position = { x, y: 0 };

			if (!positionedNodes.has(masterBranch)) {
				positionedNodes.set(masterBranch, []);
			}
			positionedNodes.get(masterBranch)!.push(masterNode);

			branchNames.forEach(branch => {
				const releases = nodeMap.get(branch);
				if (!releases || !releases.length) return;

				const subIdx = releases.findIndex(r => new Date(r.publishedAt) > new Date(masterNode.publishedAt));
				if (subIdx === -1) return;

				const node = releases[subIdx];
				const row = branchIndex.get(branch)!;
				node.position = { x, y: (row + 1) * ySpacing };

				if (!positionedNodes.has(branch)) {
					positionedNodes.set(branch, []);
				}
				if (!positionedNodes.get(branch)!.some(n => n.id === node.id)) {
					positionedNodes.get(branch)!.push(node);
				}
			});
		});

		branchNames.forEach(branch => {
			const nodes = nodeMap.get(branch) ?? [];
			const positioned = positionedNodes.get(branch) ?? [];

			for (let i = 1; i < nodes.length; i++) {
				const prev = positioned.find(n => n.id === nodes[i - 1].id);
				if (!prev) continue;

				const curr = nodes[i];
				if (positioned.some(n => n.id === curr.id)) continue;

				curr.position = {
					x: prev.position.x + xSpacing / 2,
					y: prev.position.y
				};

				positioned.push(curr);
			}

			if (positioned.length) {
				positionedNodes.set(branch, positioned);
			}
		});

		return positionedNodes;
	}

	public assignReleaseColors(releaseGroups: Map<string, ReleaseNode[]>): ReleaseNode[] {
		const now = new Date();
		const allNodes: ReleaseNode[] = [];

		for (const nodes of releaseGroups.values()) {
			nodes.forEach(release => {
				const published = new Date(release.publishedAt);

				if (release.label.toLowerCase().includes(ReleaseCoordinatePlaneComponent.GITHUB_NIGHTLY_RELEASE)) {
					release.color = 'darkblue';
				} else {
					const isMaster = release.branch === ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH;
					const fullSupportMonths = isMaster ? 12 : 3;
					const securitySupportMonths = isMaster ? 36 : 6;

					const fullSupportEnd = new Date(published);
					fullSupportEnd.setMonth(published.getMonth() + fullSupportMonths);

					const securitySupportEnd = new Date(published);
					securitySupportEnd.setMonth(published.getMonth() + securitySupportMonths);

					release.color = now <= fullSupportEnd
							? 'yellow'
							: now <= securitySupportEnd
									? 'orange'
									: 'red';
				}

				allNodes.push(release);
			});
		}

		return allNodes;
	}

	private createLinks(sortedGroups: Map<string, ReleaseNode[]>[]): ReleaseLink[] {
		const links: ReleaseLink[] = [];
		const nodeMap = new Map(this.releaseNodes.map(n => [n.id, n]));

		const masterBranch = ReleaseCoordinatePlaneComponent.GITHUB_MASTER_BRANCH;
		const masterNodes = sortedGroups[0].get(masterBranch) ?? [];

		masterNodes.sort((a, b) => a.publishedAt.getTime() - b.publishedAt.getTime());

		for (let i = 0; i < masterNodes.length - 1; i++) {
			links.push({
				id: `${masterNodes[i].id}-${masterNodes[i + 1].id}`,
				source: masterNodes[i].id,
				target: masterNodes[i + 1].id
			});
		}

		for (let i = 1; i < sortedGroups.length; i++) {
			const [_, subNodes] = [...sortedGroups[i].entries()][0];
			if (!subNodes.length) continue;

			subNodes.sort((a, b) => a.publishedAt.getTime() - b.publishedAt.getTime());

			const firstSub = subNodes[0];
			const subVersionMatch = firstSub.label.match(/^v?(\d+\.\d+)/i);
			if (!subVersionMatch) continue;
			const subMajorMinor = subVersionMatch[1];

			const matchingMaster = masterNodes
					.filter(m => {
						const masterVersionMatch = m.label.match(/^v?(\d+\.\d+)/i);
						return (
								masterVersionMatch &&
								masterVersionMatch[1] === subMajorMinor &&
								m.publishedAt.getTime() < firstSub.publishedAt.getTime()
						);
					})
					.sort((a, b) => b.publishedAt.getTime() - a.publishedAt.getTime())[0];

			if (matchingMaster) {
				links.push({
					id: `${matchingMaster.id}-${firstSub.id}`,
					source: matchingMaster.id,
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

		const nodeRadius = 20;
		const [x1, y1] = [source.position.x, source.position.y];
		const [x2, y2] = [target.position.x, target.position.y];

		if (y1 === y2) {
			return `M ${x1 + nodeRadius},${y1} L ${x2 - nodeRadius},${y2}`;
		}

		const arcRadius = Math.abs(x2 - x1);
		const sweep = x2 < x1 ? 1 : 0;

		return `M ${x1},${y1 + nodeRadius} A ${arcRadius},${arcRadius} 0 0,${sweep} ${x2 - nodeRadius},${y2}`;
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

		const width = (maxX - minX + 200);
		const height = (maxY - minY + 200);
		const x = minX - 100;
		const y = minY - 100;

		this.svgElement.nativeElement.setAttribute('viewBox', `${x} ${y} ${width} ${height}`);
	}
}
