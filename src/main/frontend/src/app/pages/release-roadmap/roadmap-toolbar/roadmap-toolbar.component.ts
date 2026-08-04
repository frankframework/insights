import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ViewMode } from '../roadmap.types';

@Component({
  selector: 'app-roadmap-toolbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './roadmap-toolbar.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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
