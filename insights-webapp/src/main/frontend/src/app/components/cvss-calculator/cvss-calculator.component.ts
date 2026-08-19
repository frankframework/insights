import {
  Component,
  Signal,
  WritableSignal,
  computed,
  inject,
  input,
  linkedSignal,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModalComponent } from '../modal/modal.component';
import { TooltipService } from '../tooltip/tooltip.service';
import {
  calculateCvssScore,
  CVSS_METRICS,
  CvssMetricKey,
  CvssResult,
  CvssVector,
  parseVectorString,
  vectorToString,
} from '../../pipes/cvss';

@Component({
  selector: 'app-cvss-calculator',
  standalone: true,
  imports: [FormsModule, ModalComponent],
  templateUrl: './cvss-calculator.component.html',
  host: { class: 'block' },
})
export class CvssCalculatorComponent {
  public readonly referenceScore = input<number | null>(null);
  public readonly referenceVector = input<string | null>(null);

  public readonly closed = output<void>();
  public readonly scoreSelected = output<number>();

  public readonly metrics = CVSS_METRICS;

  public readonly selection: WritableSignal<CvssVector> = linkedSignal<string | null, CvssVector>({
    source: this.referenceVector,
    computation: (vector) => (vector ? (parseVectorString(vector) ?? {}) : {}),
  });

  public readonly vectorInput: WritableSignal<string> = linkedSignal<string | null, string>({
    source: this.referenceVector,
    computation: (vector) => (vector && parseVectorString(vector) ? vector : ''),
  });

  public readonly vectorError: WritableSignal<string | null> = signal<string | null>(null);

  public readonly result: Signal<CvssResult | null> = computed(() => calculateCvssScore(this.selection()));
  public readonly currentVectorString: Signal<string> = computed(() => vectorToString(this.selection()));

  private readonly tooltipService = inject(TooltipService);

  public selectMetric(key: CvssMetricKey, optionKey: string): void {
    this.selection.update((current) => ({ ...current, [key]: optionKey }));
    this.vectorError.set(null);
  }

  public applyVectorInput(): void {
    const parsed = parseVectorString(this.vectorInput());
    if (!parsed) {
      this.vectorError.set(
        'Enter a complete CVSS 3.1 vector string, for example: CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H',
      );
      return;
    }

    this.selection.set(parsed);
    this.vectorError.set(null);
  }

  public close(): void {
    this.closed.emit();
  }

  public showTooltip(event: Event, title: string, description: string): void {
    this.tooltipService.show(event.currentTarget as HTMLElement, title, [{ value: description }]);
  }

  public hideTooltip(): void {
    this.tooltipService.hide();
  }

  public useScore(): void {
    const result = this.result();
    if (!result) return;

    this.scoreSelected.emit(result.score);
  }
}
