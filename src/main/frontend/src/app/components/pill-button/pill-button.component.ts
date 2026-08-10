import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';

export type PillButtonIcon = 'moon' | 'help' | 'github';

@Component({
  selector: 'app-pill-button',
  standalone: true,
  imports: [],
  templateUrl: './pill-button.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './pill-button.component.scss',
})
export class PillButtonComponent {
  readonly icon = input<PillButtonIcon>('help');
  readonly label = input('');
  readonly active = input(false);
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly tooltip = input('');

  readonly clicked = output<void>();
}
