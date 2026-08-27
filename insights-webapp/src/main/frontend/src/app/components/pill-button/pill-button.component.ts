import { Component, computed, input, output } from '@angular/core';

export type PillButtonIcon = 'moon' | 'help' | 'github' | 'list';

@Component({
  selector: 'app-pill-button',
  standalone: true,
  imports: [],
  templateUrl: './pill-button.component.html',
  host: { class: 'inline-flex items-center' },
})
export class PillButtonComponent {
  readonly icon = input<PillButtonIcon>('help');
  readonly label = input('');
  readonly active = input(false);
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly tooltip = input('');

  readonly clicked = output<void>();

  public readonly iconMask = computed(() => `url(/assets/icons/${this.loading() ? 'spinner' : this.icon()}.svg)`);
}
