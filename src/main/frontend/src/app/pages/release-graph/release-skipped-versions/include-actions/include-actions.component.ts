import { Component, EventEmitter, Input, Output } from '@angular/core';
import { serializeVersionRanges, VersionRange } from '../../../../pipes/release-range';

@Component({
  selector: 'app-include-actions',
  standalone: true,
  imports: [],
  templateUrl: './include-actions.component.html',
  styleUrl: './include-actions.component.scss',
})
export class IncludeActionsComponent {
  @Input() includableRanges: VersionRange[] = [];
  @Input() pendingCount = 0;
  @Input() alreadyIncluded = false;
  @Output() includeAll = new EventEmitter<void>();
  @Output() applyPending = new EventEmitter<void>();

  public get includeLabel(): string {
    return serializeVersionRanges(this.includableRanges);
  }

  public get includeButtonLabel(): string {
    if (this.alreadyIncluded) return 'Already included';
    return this.includableRanges.length > 3 ? 'Include all these in graph' : `Include ${this.includeLabel} in graph`;
  }

  public get pendingLabel(): string {
    return this.pendingCount === 1 ? '1 pending release' : `${this.pendingCount} pending releases`;
  }
}
