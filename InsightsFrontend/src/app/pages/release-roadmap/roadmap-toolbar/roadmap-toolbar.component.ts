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
  @Input() displayDate!: Date;
  @Output() changePeriod = new EventEmitter<number>();
  @Output() resetPeriod = new EventEmitter<void>();

  get currentPeriodLabel(): string {
    if (!this.displayDate) return '';
    const year = this.displayDate.getFullYear();
    const month = this.displayDate.getMonth();
    const half = month < 6 ? 'H1' : 'H2';
    return `${half} ${year}`;
  }
}
