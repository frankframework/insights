import { ChangeDetectionStrategy, Component, Signal, computed, input, output, signal } from '@angular/core';
import { BusinessValue } from '../../../../services/business-value.service';
import { MarkdownPipe } from '../../../../pipes/markdown.pipe';

@Component({
  selector: 'app-business-value-panel',
  standalone: true,
  imports: [MarkdownPipe],
  templateUrl: './business-value-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './business-value-panel.component.scss',
})
export class BusinessValuePanelComponent {
  public readonly businessValues = input.required<BusinessValue[]>();
  public readonly selectedBusinessValue = input<BusinessValue | null>(null);

  public readonly businessValueSelected = output<BusinessValue>();
  public readonly createClicked = output<void>();
  public readonly editClicked = output<void>();
  public readonly deleteClicked = output<{
    businessValue: BusinessValue;
    event: Event;
  }>();
  public readonly duplicateClicked = output<void>();

  public readonly businessValueSearchQuery = signal<string>('');

  public readonly filteredBusinessValues: Signal<BusinessValue[]> = computed(() => {
    const query = this.businessValueSearchQuery().toLowerCase().trim();
    let values = this.businessValues();

    if (query) {
      values = values.filter((businessValue) => businessValue.title.toLowerCase().includes(query));
    }

    return [...values].toSorted(this.sortBusinessValuesByIssueCount);
  });

  public updateSearchQuery(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.businessValueSearchQuery.set(query);
  }

  public selectBusinessValue(businessValue: BusinessValue): void {
    this.businessValueSelected.emit(businessValue);
  }

  public onCreateClick(): void {
    this.createClicked.emit();
  }

  public onEditClick(event: Event): void {
    event.stopPropagation();
    this.editClicked.emit();
  }

  public onDeleteClick(businessValue: BusinessValue, event: Event): void {
    event.stopPropagation();
    this.deleteClicked.emit({ businessValue, event });
  }

  public onDuplicateClick(): void {
    this.duplicateClicked.emit();
  }

  private sortBusinessValuesByIssueCount = (businessValueA: BusinessValue, businessValueB: BusinessValue): number =>
    (businessValueB.issues?.length || 0) - (businessValueA.issues?.length || 0);
}
