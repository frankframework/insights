import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TooltipService } from './tooltip.service';

@Component({
  selector: 'app-tooltip',
  standalone: true,
  imports: [],
  templateUrl: './tooltip.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./tooltip.component.scss'],
})
export class TooltipComponent {
  public readonly tooltip = inject(TooltipService).tooltip;
}
