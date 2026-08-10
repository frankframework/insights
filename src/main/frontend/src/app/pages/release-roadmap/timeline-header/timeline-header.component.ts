import { ChangeDetectionStrategy, Component, Signal, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-timeline-header',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './timeline-header.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./timeline-header.component.scss'],
})
export class TimelineHeaderComponent {
  public readonly months = input<Date[]>([]);
  public readonly quarters = input<{ name: string; monthCount: number }[]>([]);

  public readonly quartersGridStyle: Signal<string> = computed(() =>
    this.quarters()
      .map((quarter) => `${quarter.monthCount}fr`)
      .join(' '),
  );
}
