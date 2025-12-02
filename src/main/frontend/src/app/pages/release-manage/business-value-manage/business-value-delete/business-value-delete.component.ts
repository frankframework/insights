import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { BusinessValue, BusinessValueService } from '../../../../services/business-value.service';

@Component({
  selector: 'app-business-value-delete',
  standalone: true,
  imports: [CommonModule, ModalComponent],
  templateUrl: './business-value-delete.component.html',
  styleUrl: './business-value-delete.component.scss',
})
export class BusinessValueDeleteComponent {
  @Input({ required: true }) businessValue!: BusinessValue;
  @Output() closed = new EventEmitter<void>();
  @Output() businessValueDeleted = new EventEmitter<string>(); // Emits ID of deleted item

  public isDeleting = signal<boolean>(false);
  public errorMessage = signal<string>('');

  private businessValueService = inject(BusinessValueService);

  public close(): void {
    this.closed.emit();
  }

  public confirmDelete(): void {
    this.isDeleting.set(true);
    this.errorMessage.set('');

    this.businessValueService.deleteBusinessValue(this.businessValue.id).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.businessValueDeleted.emit(this.businessValue.id);
        this.close();
      },
      error: (error) => {
        this.isDeleting.set(false);
        this.errorMessage.set(error.error?.message || 'Failed to delete business value');
      },
    });
  }
}
