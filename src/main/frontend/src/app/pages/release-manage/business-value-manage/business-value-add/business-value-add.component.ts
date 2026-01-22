import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { BusinessValue, BusinessValueService } from '../../../../services/business-value.service';

@Component({
  selector: 'app-business-value-add',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './business-value-add.component.html',
  styleUrl: './business-value-add.component.scss',
})
export class BusinessValueAddComponent {
  @Output() closed = new EventEmitter<void>();
  @Output() businessValueCreated = new EventEmitter<BusinessValue>();

  public name = signal<string>('');
  public description = signal<string>('');
  public isSaving = signal<boolean>(false);
  public errorMessage = signal<string>('');

  private businessValueService = inject(BusinessValueService);

  public close(): void {
    this.closed.emit();
  }

  public save(): void {
    const nameValue = this.name().trim();
    const descriptionValue = this.description().trim();

    // Basic required check
    if (!nameValue || !descriptionValue) {
      this.errorMessage.set('Both title and description are required');
      return;
    }

    // Max 255 characters for title
    if (nameValue.length > 255) {
      this.errorMessage.set(`Title cannot exceed 255 characters (current: ${nameValue.length})`);
      return;
    }

    if (descriptionValue.length > 1000) {
      this.errorMessage.set(`Description cannot exceed 1000 characters (current: ${descriptionValue.length})`);
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set('');

    this.businessValueService
      .createBusinessValue(nameValue, descriptionValue)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (businessValue) => {
          this.businessValueCreated.emit(businessValue);
          this.close();
        },
        error: (error) => this.errorMessage.set(error.error?.message || 'Failed to create business value'),
      });
  }

  public updateName(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.name.set(value);
    if (this.errorMessage()) {
      this.errorMessage.set('');
    }
  }

  public updateDescription(event: Event): void {
    const value = (event.target as HTMLTextAreaElement).value;
    this.description.set(value);
  }
}
