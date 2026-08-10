import { Component, Signal, computed, input, output } from '@angular/core';
import { serializeVersionRanges, VersionRange } from '../../../../pipes/release-range';

@Component({
  selector: 'app-include-actions',
  standalone: true,
  imports: [],
  templateUrl: './include-actions.component.html',
  styleUrl: './include-actions.component.scss',
})
export class IncludeActionsComponent {
  public readonly includableRanges = input<VersionRange[]>([]);
  public readonly pendingCount = input<number>(0);
  public readonly alreadyIncluded = input<boolean>(false);

  public readonly includeAll = output<void>();
  public readonly applyPending = output<void>();

  public readonly includeLabel: Signal<string> = computed(() => serializeVersionRanges(this.includableRanges()));

  public readonly includeButtonLabel: Signal<string> = computed(() => {
    if (this.alreadyIncluded()) return 'Already included';
    return this.includableRanges().length > 3
      ? 'Include all these in graph'
      : `Include ${this.includeLabel()} in graph`;
  });

  public readonly pendingLabel: Signal<string> = computed(() => {
    const pendingCount = this.pendingCount();
    return pendingCount === 1 ? '1 pending release' : `${pendingCount} pending releases`;
  });
}
