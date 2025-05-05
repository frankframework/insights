import { Component, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxGraphModule, GraphComponent, Node, Edge } from '@swimlane/ngx-graph';

@Component({
	selector: 'app-release-graph',
	standalone: true,
	imports: [CommonModule, NgxGraphModule],
	template: `
		<div class="graph-container">
			<div class="scroll-wrapper" #scrollWrapper>
				<ngx-graph
						#graph
						[layout]="'none'"
						[nodes]="nodes"
						[links]="links"
						[draggingEnabled]="false"
						[autoZoom]="false"
						[enableZoom]="true"
						[panOnZoom]="true"
				>
					<ng-template #defsTemplate></ng-template>

					<ng-template #nodeTemplate let-node>
						<svg:g class="node" [attr.transform]="'translate(' + (node.position?.x || 0) + ',' + (node.position?.y || 0) + ')'">
							<circle r="20" [attr.fill]="node.data.color" stroke="#333" stroke-width="2"></circle>
							<text text-anchor="middle" y="40" fill="black" font-size="14">
								{{ node.label }}
							</text>
						</svg:g>
					</ng-template>

					<ng-template #linkTemplate let-link>
						<svg:g class="edge">
							<svg:path
									class="line"
									stroke="#999"
									stroke-width="2"
									fill="none"
									[attr.d]="getCustomPath(link)"
							/>
						</svg:g>
					</ng-template>

				</ngx-graph>
			</div>
		</div>
	`,
	styles: [`
		.graph-container {
			width: 100vw;
			height: 100vh;
			overflow: hidden;
			display: flex;
			align-items: center;
			justify-content: center;
			background: #f5f5f5;
		}

		.scroll-wrapper {
			width: 100vw;
			height: 100%;
			overflow-x: auto;
			overflow-y: hidden;
			display: flex;
			align-items: center;
			scroll-behavior: smooth;
		}

		ngx-graph {
			min-width: 1800px;
			height: 100%;
		}
	`]
})
export class ReleaseGraphComponent implements AfterViewInit {
	@ViewChild('scrollWrapper') scrollWrapper!: ElementRef<HTMLDivElement>;
	@ViewChild('graph') graphComponent!: GraphComponent;

	nodes: Node[] = [
		{ id: '7.7.0', label: '7.7.0', position: { x: 0, y: 0 }, data: { color: '#ccc' } },
		{ id: '7.8.0', label: '7.8.0', position: { x: 500, y: 0 }, data: { color: '#4fc3f7' } },
		{ id: '7.9.0', label: '7.9.0', position: { x: 1000, y: 0 }, data: { color: '#ff9800' } },

		{ id: '7.7.1', label: '7.7.1', position: { x: 75, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.2', label: '7.7.2', position: { x: 150, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.3', label: '7.7.3', position: { x: 225, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.4', label: '7.7.4', position: { x: 300, y: 100 }, data: { color: '#ccc' } },
		{ id: '7.7.5', label: '7.7.5', position: { x: 375, y: 100 }, data: { color: '#ccc' } },

		{ id: '7.8.1', label: '7.8.1', position: { x: 550, y: 200 }, data: { color: '#4fc3f7' } },
		{ id: '7.8.2', label: '7.8.2', position: { x: 625, y: 200 }, data: { color: '#4fc3f7' } },
		{ id: '7.8.3', label: '7.8.3', position: { x: 700, y: 200 }, data: { color: '#4fc3f7' } },
		{ id: '7.8.4', label: '7.8.4', position: { x: 775, y: 200 }, data: { color: '#4fc3f7' } },
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
		{ id: 'l11', source: '7.8.3', target: '7.8.4' },
	];

	ngAfterViewInit(): void {
		setTimeout(() => {
			const el = this.scrollWrapper.nativeElement;
			el.scrollLeft = el.scrollWidth / 2;
			this.graphComponent.zoomTo(1.25);
		});
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

		return `M${x1},${y1} C${x1},${y1 + 50} ${x2},${y2 - 50} ${x2},${y2}`;
	}
}
