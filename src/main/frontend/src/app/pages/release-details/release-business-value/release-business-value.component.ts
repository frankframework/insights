import { Component, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BusinessValue } from '../../../services/business-value.service';
import { ReleaseBusinessValueModalComponent } from '../release-business-value-modal/release-business-value-modal.component';

@Component({
  selector: 'app-release-business-value',
  standalone: true,
  imports: [CommonModule, ReleaseBusinessValueModalComponent],
  templateUrl: './release-business-value.component.html',
  styleUrl: './release-business-value.component.scss',
})
export class ReleaseBusinessValueComponent {
  @Input() businessValues: BusinessValue[] | null = null;

  public selectedBusinessValue = signal<BusinessValue | null>(null);

  public openBusinessValueModal(businessValue: BusinessValue): void {
    this.selectedBusinessValue.set(businessValue);
  }

  public closeModal(): void {
    this.selectedBusinessValue.set(null);
  }
}
