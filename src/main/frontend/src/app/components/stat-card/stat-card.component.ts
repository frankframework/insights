import { Component, computed, input, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': 'hostClasses()' },
})
export class StatCardComponent {
  public readonly label = input.required<string>();
  public readonly value = input.required<string | number>();
  public readonly badgeClass = input<string | null>(null);

  public readonly hostClasses = computed(() => {
    const badge = this.badgeClass();
    return badge ? `stat-card priority-card ${badge}` : 'stat-card';
  });
}
