import { Component, input, signal } from '@angular/core';
import { BusinessValue } from '../../../services/business-value.service';
import { ReleaseBusinessValueModalComponent } from '../release-business-value-modal/release-business-value-modal.component';
import { MarkdownPipe } from '../../../pipes/markdown.pipe';

@Component({
  selector: 'app-release-business-value',
  standalone: true,
  imports: [ReleaseBusinessValueModalComponent, MarkdownPipe],
  templateUrl: './release-business-value.component.html',
})
export class ReleaseBusinessValueComponent {
  public readonly businessValues = input<BusinessValue[] | null>(null);

  public readonly selectedBusinessValue = signal<BusinessValue | null>(null);

  public openBusinessValueModal(businessValue: BusinessValue): void {
    this.selectedBusinessValue.set(businessValue);
  }

  public closeModal(): void {
    this.selectedBusinessValue.set(null);
  }
}
