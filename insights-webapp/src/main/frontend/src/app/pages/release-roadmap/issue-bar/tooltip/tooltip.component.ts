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

  private tooltipService = inject(TooltipService);

  constructor() {
    this.tooltipState$ = this.tooltipService.tooltipState$;
  }
}
