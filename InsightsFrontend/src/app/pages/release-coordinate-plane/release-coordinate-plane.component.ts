import { Component, AfterViewInit, ViewChild, ElementRef } from '@angular/core';

type Position = {
	x: number;
	y: number;
}

interface ReleaseNode {
	id: string;
	label: string;
	position: { x: number; y: number };
	data: { color: string };
}

interface ReleaseLink {
	id: string,
	source: string,
	target: string
}

@Component({
	selector: 'app-release-coordinate-plane',
	templateUrl: './release-coordinate-plane.component.html',
	styleUrls: ['./release-coordinate-plane.component.scss']
})
export class ReleaseCoordinatePlaneComponent implements AfterViewInit {
	@ViewChild('svgElement') svgElement!: ElementRef<SVGElement>;

	protected viewBox: string = "0 0 500 500";

	nodes: ReleaseNode[] = [
		{ id: '7.7.0', label: '7.7.0', position: { x: 0, y: 0 }, data: { color: '#ccc' } },
		{ id: '7.8.0', label: '7.8.0', position: { x: 400, y: 0 }, data: { color: '#4fc3f7' } },
		{ id: '7.9.0', label: '7.9.0', position: { x: 600, y: 0 }, data: { color: '#ff9800' } },
		{ id: '7.7.1', label: '7.7.1', position: { x: 75, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.2', label: '7.7.2', position: { x: 150, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.3', label: '7.7.3', position: { x: 225, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.4', label: '7.7.4', position: { x: 300, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.5', label: '7.7.5', position: { x: 375, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.8.1', label: '7.8.1', position: { x: 500, y: 200 }, data: { color: '#4fc3f7' } },
		{ id: '7.8.2', label: '7.8.2', position: { x: 600, y: 200 }, data: { color: '#4fc3f7' } },
		{ id: '7.8.3', label: '7.8.3', position: { x: 700, y: 200 }, data: { color: '#4fc3f7' } },
		{ id: '7.8.4', label: '7.8.4', position: { x: 800, y: 200 }, data: { color: '#4fc3f7' } }
	];

	links: ReleaseLink[] = [
		{ id: 'l1', source: '7.7.0', target: '7.8.0' },
		{ id: 'l2', source: '7.8.0', target: '7.9.0' },
		{ id: 'l3', source: '7.7.0', target: '7.7.1' },
		{ id: 'l4', source: '7.7.1', target: '7.7.2' },
		{ id: 'l5', source: '7.7.2', target: '7.7.3' },
		{ id: 'l6', source: '7.7.3', target: '7.7.4' },
		{ id: 'l7', source: '7.7.4', target: '7.7.5' },
		{ id: 'l8', source: '7.8.0', target: '7.8.1' },
		{ id: 'l9', source: '7.8.1', target: '7.8.2' },
		{ id: 'l10', source: '7.8.2', target: '7.8.3' },
		{ id: 'l11', source: '7.8.3', target: '7.8.4' }
	];

	ngAfterViewInit(): void {
		setTimeout(() => {
			this.centerGraph();
		}, 0);
	}

	centerGraph() {
		const minX = Math.min(...this.nodes.map(node => node.position.x));
		const maxX = Math.max(...this.nodes.map(node => node.position.x));
		const minY = Math.min(...this.nodes.map(node => node.position.y));
		const maxY = Math.max(...this.nodes.map(node => node.position.y));

		const graphWidth = maxX - minX;
		const graphHeight = maxY - minY;

		let viewBoxX = minX;
		let viewBoxY = minY;
		let viewBoxWidth = graphWidth;
		let viewBoxHeight = graphHeight;

		const zoomOutFactor = 2;

		viewBoxWidth *= zoomOutFactor;
		viewBoxHeight *= zoomOutFactor;

		viewBoxX -= (viewBoxWidth - (graphWidth)) / 2;
		viewBoxY -= (viewBoxHeight - (graphHeight)) / 2;

		this.svgElement.nativeElement.setAttribute(
				'viewBox',
				`${viewBoxX} ${viewBoxY} ${viewBoxWidth} ${viewBoxHeight}`
		);
	}

	onWheel(event: WheelEvent): void {
		event.preventDefault();
	}

	getCustomPath(link: { source: string, target: string }): string {
		const sourceNode = this.nodes.find(n => n.id === link.source);
		const targetNode = this.nodes.find(n => n.id === link.target);

		const sourcePos = sourceNode?.position;
		const targetPos = targetNode?.position;

		if (!sourcePos || !targetPos) return '';

		const { x: x1, y: y1 }: Position = sourcePos;
		const { x: x2, y: y2 }: Position = targetPos;

		const releaseNodeRadius = 20;

		const X1 = x1 + releaseNodeRadius * Math.cos(Math.atan2(y2 - y1, x2 - x1));
		const Y1 = y1 + releaseNodeRadius * Math.sin(Math.atan2(y2 - y1, x2 - x1));
		const X2 = x2 - releaseNodeRadius * Math.cos(Math.atan2(y2 - y1, x2 - x1));
		const Y2 = y2 - releaseNodeRadius * Math.sin(Math.atan2(y2 - y1, x2 - x1));

		if (y1 === y2) {
			return `M${X1},${Y1} L${X2},${Y2}`;
		}

		return `M${X1},${Y1} C${X1},${Y1 - 20} ${X2},${Y2 - 20} ${X2},${Y2}`;
	}
}
