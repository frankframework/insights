import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { BusinessValue, BusinessValueService } from '../../../services/business-value.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-release-business-value',
  imports: [],
  templateUrl: './release-business-value.component.html',
  styleUrl: './release-business-value.component.scss',
})
export class ReleaseBusinessValueComponent implements OnChanges {
  @Input() releaseId?: string;

  public businessValues = signal<BusinessValue[]>([]);
  public isLoadingBusinessValues = signal<boolean>(false);

  private businessValueService = inject(BusinessValueService);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['releaseId'] && this.releaseId) {
      this.fetchBusinessValues();
    }
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
