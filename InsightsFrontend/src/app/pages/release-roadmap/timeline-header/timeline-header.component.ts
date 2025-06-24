import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-timeline-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './timeline-header.component.html',
  styleUrls: ['./timeline-header.component.scss'],
})
export class TimelineHeaderComponent {
  @Input() months: Date[] = [];

  public quartersGridStyle = '';
  private _quarters: { name: string; monthCount: number }[] = [];

  get quarters(): { name: string; monthCount: number }[] {
    return this._quarters;
  }

  @Input()
  set quarters(value: { name: string; monthCount: number }[]) {
    this._quarters = value;
    this.quartersGridStyle = this._quarters.map((q) => `${q.monthCount}fr`).join(' ');
  }
}
