import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-roadmap-toolbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './roadmap-toolbar.component.html',
  styleUrls: ['./roadmap-toolbar.component.scss'],
})
export class RoadmapToolbarComponent {
  @Input() public periodLabel = '';
  @Output() public readonly changePeriod = new EventEmitter<number>();
  @Output() public readonly resetPeriod = new EventEmitter<void>();
  @Output() public readonly openFutureDetails = new EventEmitter<void>();
}
