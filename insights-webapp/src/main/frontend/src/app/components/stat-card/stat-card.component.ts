import { Component, computed, input } from '@angular/core';

const BADGE_TINTS: Record<string, { card: string; value: string }> = {
  'badge-critical': { card: 'bg-rose-50 border-rose-200', value: 'text-rose-700' },
  'badge-high': { card: 'bg-orange-50 border-orange-200', value: 'text-orange-700' },
  'badge-medium': { card: 'bg-yellow-50 border-yellow-200', value: 'text-yellow-700' },
  'badge-low': { card: 'bg-blue-50 border-blue-200', value: 'text-blue-700' },
};

const NEUTRAL_TINT = { card: 'bg-gray-50 border-gray-100', value: 'text-gray-900' };

@Component({
  selector: 'app-stat-card',
  standalone: true,
  templateUrl: './stat-card.component.html',
  host: { '[class]': 'hostClasses()' },
})
export class StatCardComponent {
  public readonly label = input.required<string>();
  public readonly value = input.required<string | number>();
  public readonly badgeClass = input<string | null>(null);

  public readonly hostClasses = computed(() => {
    const badge = this.badgeClass();
    const base = 'stat-card flex min-w-16 flex-col gap-0.5 rounded-lg border px-3 py-1.5';

    return [base, badge ? `priority-card ${badge}` : '', this.tint().card].filter(Boolean).join(' ');
  });

  public readonly valueClasses = computed(() => `stat-value text-sm font-semibold ${this.tint().value}`);

  private readonly tint = computed(() => {
    const badge = this.badgeClass();
    return (badge && BADGE_TINTS[badge]) || NEUTRAL_TINT;
  });
}
