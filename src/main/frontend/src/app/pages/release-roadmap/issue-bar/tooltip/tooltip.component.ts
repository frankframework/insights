import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { TooltipData, TooltipService } from './tooltip.service';

@Component({
  selector: 'app-tooltip',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tooltip.component.html',
  styleUrls: ['./tooltip.component.scss'],
})
export class TooltipComponent {
  public tooltipState$: Observable<TooltipData | null>;
  private tooltipHover = false;

  private tooltipService = inject(TooltipService);

  constructor() {
    this.tooltipState$ = this.tooltipService.tooltipState$;
  }

  public onTooltipMouseEnter(): void {
    this.tooltipHover = true;
  }

  public onTooltipMouseLeave(): void {
    this.tooltipHover = false;
    // Only hide if it's a release graph tooltip and not being hovered
    const currentState = this.tooltipService.tooltipSubject.value;
    if (currentState?.isReleaseGraph) {
      // Small delay to allow user to move back to tooltip
      setTimeout(() => {
        if (!this.tooltipHover) {
          this.tooltipService.hide();
        }
      }, 100);
    }
  }
}
