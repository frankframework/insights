import { Component, input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BusinessValue } from '../../../../services/business-value.service';

@Component({
  selector: 'app-business-value-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './business-value-panel.component.html',
  styleUrl: './business-value-panel.component.scss',
})
export class BusinessValuePanelComponent {
  public businessValues = input.required<BusinessValue[]>();
  public selectedBusinessValue = input<BusinessValue | null>(null);

  @Output() businessValueSelected = new EventEmitter<BusinessValue>();
  @Output() createClicked = new EventEmitter<void>();
  @Output() editClicked = new EventEmitter<void>();
  @Output() deleteClicked = new EventEmitter<{ businessValue: BusinessValue; event: Event }>();

  public businessValueSearchQuery = signal<string>('');

  public filteredBusinessValues = computed(() => {
    const query = this.businessValueSearchQuery().toLowerCase().trim();
    let values = this.businessValues();

    if (query) {
      values = values.filter((bv) => this.filterBusinessValuesByQuery(bv, query));
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

  private filterBusinessValuesByQuery(bv: BusinessValue, query: string): boolean {
    return bv.title.toLowerCase().includes(query);
  }

  private sortBusinessValuesByIssueCount = (a: BusinessValue, b: BusinessValue): number => {
    const countA = a.issues?.length || 0;
    const countB = b.issues?.length || 0;
    return countB - countA;
  };
}
