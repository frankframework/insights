import { Component, EventEmitter, Input, Output, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { BusinessValue, BusinessValueService } from '../../../../services/business-value.service';

@Component({
  selector: 'app-business-value-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './business-value-edit.component.html',
  styleUrl: './business-value-edit.component.scss',
})
export class BusinessValueEditComponent {
  @Output() closed = new EventEmitter<void>();
  @Output() businessValueUpdated = new EventEmitter<BusinessValue>();

  public _businessValue!: BusinessValue;
  public name = signal<string>('');
  public description = signal<string>('');
  public isSaving = signal<boolean>(false);
  public errorMessage = signal<string>('');

  // eslint-disable-next-line unicorn/consistent-function-scoping
  public isFormValidAndChanged = computed<boolean>(() => {
    const currentName = this.name().trim();
    const currentDescription = this.description().trim();

    const isRequiredFilled = currentName.length > 0 && currentDescription.length > 0;

    const hasChanged = currentName !== this.originalName() || currentDescription !== this.originalDescription();

    return isRequiredFilled && hasChanged;
  });

  private originalName = signal<string>('');
  private originalDescription = signal<string>('');

  private businessValueService = inject(BusinessValueService);

  get businessValue(): BusinessValue {
    return this._businessValue;
  }

  @Input({ required: true })
  set businessValue(bv: BusinessValue) {
    this._businessValue = bv;
    this.name.set(bv.title);
    this.description.set(bv.description);
    this.originalName.set(bv.title);
    this.originalDescription.set(bv.description);
  }

  public close(): void {
    this.closed.emit();
  }

  public save(): void {
    const nameValue = this.name().trim();
    const descriptionValue = this.description().trim();

    if (!nameValue || !descriptionValue) {
      this.errorMessage.set('Both title and description are required');
      return;
    }

    if (!this.isFormValidAndChanged()) {
      return;
    }

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
      .updateBusinessValue(this._businessValue.id, nameValue, descriptionValue)
      .pipe(finalize(() => this.isSaving.set(false)))
      .subscribe({
        next: (businessValue) => {
          this.businessValueUpdated.emit(businessValue);
          this.close();
        },
        error: (error) => this.errorMessage.set(error.error?.message || 'Failed to update business value'),
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
