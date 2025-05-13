import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ReleaseService } from '../../services/release.service';
import { catchError, map, of, tap } from 'rxjs';
import {ReleaseNode, ReleaseNodeService } from './release-node.service';
import {ReleaseLink, ReleaseLinkService} from "./release-link.service";

@Component({
	selector: 'app-release-graph',
	standalone: true,
	templateUrl: './release-graph.component.html',
	styleUrls: ['./release-graph.component.scss']
})
export class ReleaseGraphComponent implements OnInit {
	@ViewChild('svgElement') svgElement!: ElementRef<SVGElement>;

	public static readonly GITHUB_MASTER_BRANCH = 'master';

	public releaseNodes: ReleaseNode[] = [];
	public releaseLinks: ReleaseLink[] = [];
	public scale = 1;
	public translateX = 0;
	public translateY = 0;
	public viewBox = '0 0 0 0';

	private isPanning = false;
	private startPan = { x: 0, y: 0 };

	constructor(
		private releaseService: ReleaseService,
		private nodeService: ReleaseNodeService,
		private linkService: ReleaseLinkService
	) {}

	ngOnInit(): void {
		this.getAllReleases();
	}

	private getAllReleases(): void {
		this.releaseService.getAllReleases().pipe(
				map(record => Object.values(record).flat()),
				map(releases => this.nodeService.sortReleases(releases)),
				tap(sortedGroups => this.buildReleaseGraph(sortedGroups)),
				catchError(err => {
					console.error('Failed to load releases:', err);
					return of([]);
				})
		).subscribe();
	}

	private buildReleaseGraph(sortedGroups: Map<string, ReleaseNode[]>[]): void {
		const releaseNodeMap = this.nodeService.calculateReleaseCoordinates(sortedGroups);
		this.releaseNodes = this.nodeService.assignReleaseColors(releaseNodeMap);
		this.releaseLinks = this.linkService.createLinks(sortedGroups);

		setTimeout(() => this.centerGraph(), 0);
	}

	public getCustomPath(link: ReleaseLink): string {
		const source = this.releaseNodes.find(n => n.id === link.source);
		const target = this.releaseNodes.find(n => n.id === link.target);
		if (!source || !target) return '';

		const releaseNodeRadiusWithMargin = 25;
		const [x1, y1] = [source.position.x, source.position.y];
		const [x2, y2] = [target.position.x, target.position.y];

		if (y1 === y2) {
			return `M ${x1 + releaseNodeRadiusWithMargin},${y1} L ${x2 - releaseNodeRadiusWithMargin},${y2}`;
		}

		const verticalDirection = y2 > y1 ? 1 : -1;
		const cornerY = y2 - verticalDirection * releaseNodeRadiusWithMargin;
		const horizontalSweep = x2 > x1 ? 0 : 1;

		return [
			`M ${x1},${y1 + releaseNodeRadiusWithMargin}`,
			`L ${x1},${cornerY}`,
			`A ${releaseNodeRadiusWithMargin},${releaseNodeRadiusWithMargin} 0 0,${horizontalSweep} ${x1 + (horizontalSweep ? -releaseNodeRadiusWithMargin : releaseNodeRadiusWithMargin)},${y2}`,
			`L ${x2 - releaseNodeRadiusWithMargin},${y2}`,
		].join(' ');
	}

	public onWheel(event: WheelEvent): void {
		event.preventDefault();

		const [mouseX, mouseY] = this.getMousePosition(event);
		const svgX = (mouseX - this.translateX) / this.scale;
		const svgY = (mouseY - this.translateY) / this.scale;

		const delta = event.deltaY < 0 ? 1.1 : 1 / 1.1;
		this.scale *= delta;
		this.translateX = mouseX - svgX * this.scale;
		this.translateY = mouseY - svgY * this.scale;
	}

	private getMousePosition(event: MouseEvent | WheelEvent): [number, number] {
		const rect = this.svgElement.nativeElement.getBoundingClientRect();
		return [event.clientX - rect.left, event.clientY - rect.top];
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

	private centerGraph(): void {
		if (!this.svgElement || !this.releaseNodes.length) return;
		this.viewBox = this.calculateViewBox(this.releaseNodes, 100);
	}

	private calculateViewBox(nodes: ReleaseNode[], padding: number): string {
		const xs = nodes.map(n => n.position.x);
		const ys = nodes.map(n => n.position.y);
		const minX = Math.min(...xs);
		const maxX = Math.max(...xs);
		const minY = Math.min(...ys);
		const maxY = Math.max(...ys);
		const width = maxX - minX + padding * 2;
		const height = maxY - minY + padding * 2;
		const x = minX - padding;
		const y = minY - padding;
		return `${x} ${y} ${width} ${height}`;
	}
}
