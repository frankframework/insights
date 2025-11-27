import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BusinessValue, BusinessValueService } from '../../../services/business-value.service';
import { ReleaseBusinessValueModalComponent } from '../release-business-value-modal/release-business-value-modal.component';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-release-business-value',
  standalone: true,
  imports: [CommonModule, ReleaseBusinessValueModalComponent],
  templateUrl: './release-business-value.component.html',
  styleUrl: './release-business-value.component.scss',
})
export class ReleaseBusinessValueComponent implements OnChanges {
  @Input() releaseId?: string;

  public businessValues = signal<BusinessValue[]>([]);
  public isLoadingBusinessValues = signal<boolean>(false);
  public selectedBusinessValueId = signal<string | null>(null);

  private businessValueService = inject(BusinessValueService);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['releaseId'] && this.releaseId) {
      this.fetchBusinessValues();
    }
  }

  public openBusinessValueModal(businessValue: BusinessValue): void {
    this.selectedBusinessValueId.set(businessValue.id);
  }

  public closeModal(): void {
    this.selectedBusinessValueId.set(null);
  }

  private fetchBusinessValues(): void {
    if (!this.releaseId) return;

    this.isLoadingBusinessValues.set(true);
    this.businessValueService
      .getBusinessValuesByReleaseId(this.releaseId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load business values:', error);
          return of([]);
        }),
      )
      .subscribe((businessValues) => {
        this.businessValues.set(businessValues);
        this.isLoadingBusinessValues.set(false);
      });
  }
}
