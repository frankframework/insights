import { Component, EventEmitter, Input, Output, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { BusinessValue, BusinessValueService } from '../../../../services/business-value.service';

@Component({
  selector: 'app-business-value-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './business-value-edit.component.html',
  styleUrl: './business-value-edit.component.scss',
})
export class BusinessValueEditComponent implements OnInit {
  @Input() businessValue!: BusinessValue;
  @Output() closed = new EventEmitter<void>();
  @Output() businessValueUpdated = new EventEmitter<BusinessValue>();

  public name = signal<string>('');
  public description = signal<string>('');
  public isSaving = signal<boolean>(false);
  public errorMessage = signal<string>('');

  private businessValueService = inject(BusinessValueService);

  ngOnInit(): void {
    if (this.businessValue) {
      this.name.set(this.businessValue.title);
      this.description.set(this.businessValue.description);
    }
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

    if (!nameValue || !descriptionValue) {
      this.errorMessage.set('Both title and description are required');
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

    this.businessValueService.updateBusinessValue(this.businessValue.id, nameValue, descriptionValue).subscribe({
      next: (businessValue) => {
        this.isSaving.set(false);
        this.businessValueUpdated.emit(businessValue);
        this.close();
      },
      error: (error) => {
        this.isSaving.set(false);
        this.errorMessage.set(error.error?.message || 'Failed to update business value');
      },
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
