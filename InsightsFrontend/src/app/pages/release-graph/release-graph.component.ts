import {
	Component,
	AfterViewInit,
	ViewChild,
	ElementRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
	NgxGraphModule,
	GraphComponent,
	Node,
	Edge
} from '@swimlane/ngx-graph';

@Component({
	selector: 'app-release-graph',
	standalone: true,
	imports: [CommonModule, NgxGraphModule],
	templateUrl: './release-graph.component.html',
	styleUrls: ['./release-graph.component.scss']
})
export class ReleaseGraphComponent implements AfterViewInit {
	@ViewChild('scrollWrapper') scrollWrapper!: ElementRef<HTMLDivElement>;
	@ViewChild('graph') graphComponent!: GraphComponent;

	nodes: Node[] = [
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

	links: Edge[] = [
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
			const el = this.scrollWrapper.nativeElement;
			el.scrollLeft = el.scrollWidth / 2;
			this.graphComponent.zoomTo(1.25);
		}, 0);
	}


	getCustomPath(link: Edge): string {
		const sourceNode = this.nodes.find(n => n.id === link.source);
		const targetNode = this.nodes.find(n => n.id === link.target);

		const sourcePos = sourceNode?.position;
		const targetPos = targetNode?.position;

		if (!sourcePos || !targetPos) return '';

		const { x: x1, y: y1 } = sourcePos;
		const { x: x2, y: y2 } = targetPos;

		if (y1 === y2) {
			return `M${x1},${y1} L${x2},${y2}`;
		}

		return `M${x1},${y1} C${x1},${y1 - 20} ${x2},${y2 - 50} ${x2},${y2}`;
	}
}
