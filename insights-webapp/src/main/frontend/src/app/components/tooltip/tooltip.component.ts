import { Component, inject } from '@angular/core';
import { TooltipService } from './tooltip.service';

@Component({
  selector: 'app-tooltip',
  standalone: true,
  imports: [],
  templateUrl: './tooltip.component.html',
})
export class TooltipComponent {
  public readonly tooltip = inject(TooltipService).tooltip;
}
