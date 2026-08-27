import { NgOptimizedImage } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { ViewMode } from '../roadmap.types';

@Component({
  selector: 'app-roadmap-toolbar',
  standalone: true,
  imports: [
    NgOptimizedImage
  ],
  templateUrl: './roadmap-toolbar.component.html',
})
export class RoadmapToolbarComponent {
  public readonly periodLabel = input('');
  public readonly viewMode = input<ViewMode>(ViewMode.QUARTERLY);
  public readonly changePeriod = output<number>();
  public readonly resetPeriod = output<void>();
  public readonly toggleViewMode = output<void>();

  public ViewMode = ViewMode;
}
