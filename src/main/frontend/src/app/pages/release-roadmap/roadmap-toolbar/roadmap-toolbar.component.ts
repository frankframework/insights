import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ViewMode } from '../release-roadmap.component';

@Component({
  selector: 'app-roadmap-toolbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './roadmap-toolbar.component.html',
  styleUrls: ['./roadmap-toolbar.component.scss'],
})
export class RoadmapToolbarComponent {
  @Input() public periodLabel = '';
  @Input() public viewMode: ViewMode = ViewMode.QUARTERLY;
  @Output() public readonly changePeriod = new EventEmitter<number>();
  @Output() public readonly resetPeriod = new EventEmitter<void>();
  @Output() public readonly toggleViewMode = new EventEmitter<void>();

  public ViewMode = ViewMode;
}
