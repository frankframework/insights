import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { finalize } from 'rxjs';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { BusinessValue, BusinessValueService } from '../../../../services/business-value.service';

@Component({
  selector: 'app-business-value-delete',
  standalone: true,
  imports: [TitleCasePipe, ModalComponent],
  templateUrl: './business-value-delete.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './business-value-delete.component.scss',
})
export class BusinessValueDeleteComponent {
  public readonly businessValue = input.required<BusinessValue>();

  public readonly closed = output<void>();
  public readonly businessValueDeleted = output<string>();

  public readonly isDeleting = signal<boolean>(false);
  public readonly errorMessage = signal<string>('');

  private readonly businessValueService = inject(BusinessValueService);

  public close(): void {
    this.closed.emit();
  }

  public confirmDelete(): void {
    this.isDeleting.set(true);
    this.errorMessage.set('');

    this.businessValueService
      .deleteBusinessValue(this.businessValue().id)
      .pipe(finalize(() => this.isDeleting.set(false)))
      .subscribe({
        next: () => {
          this.businessValueDeleted.emit(this.businessValue().id);
          this.close();
        },
        error: (error) => this.errorMessage.set(error.error?.message || 'Failed to delete business value'),
      });
  }
}
