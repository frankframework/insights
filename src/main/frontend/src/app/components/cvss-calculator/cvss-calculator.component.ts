import {
  Component,
  computed,
  EventEmitter,
  inject,
  Input,
  OnInit,
  Output,
  Signal,
  signal,
  WritableSignal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
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
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './cvss-calculator.component.html',
  styleUrl: './cvss-calculator.component.scss',
})
export class CvssCalculatorComponent implements OnInit {
  @Input() referenceScore: number | null = null;
  @Input() referenceVector: string | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() scoreSelected = new EventEmitter<number>();

  public readonly metrics = CVSS_METRICS;

  public vectorInput = '';
  public vectorError: WritableSignal<string | null> = signal(null);

  public selection: WritableSignal<CvssVector> = signal({});

  public result: Signal<CvssResult | null> = computed(() => calculateCvssScore(this.selection()));
  public currentVectorString: Signal<string> = computed(() => vectorToString(this.selection()));

  private tooltipService = inject(TooltipService);

  ngOnInit(): void {
    if (!this.referenceVector) return;

    const parsed = parseVectorString(this.referenceVector);
    if (!parsed) return;

    this.vectorInput = this.referenceVector;
    this.selection.set(parsed);
  }

  public selectMetric(key: CvssMetricKey, optionKey: string): void {
    this.selection.update((current) => ({ ...current, [key]: optionKey }));
    this.vectorError.set(null);
  }

  public applyVectorInput(): void {
    const parsed = parseVectorString(this.vectorInput);
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
