import { Component, computed, input, output } from '@angular/core';

export type PillButtonIcon = 'moon' | 'help' | 'github' | 'list';

const PILL_BASE =
  'flex h-9 cursor-pointer items-center justify-start gap-2 rounded-full border py-1.5 pr-3.5 pl-2.5 ' +
  'transition-all duration-200 ease-in-out enabled:active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60';

const PILL_ACTIVE =
  'active border-blue-900 bg-blue-900 shadow-glow-brand ' +
  'enabled:hover:border-brand-hover enabled:hover:bg-brand-hover';

const PILL_IDLE = 'border-gray-300 bg-white shadow-e2 enabled:hover:bg-gray-50 enabled:hover:shadow-e4';

const ICON_BASE = 'size-5 shrink-0 mask-contain mask-center mask-no-repeat transition-colors duration-200 ease-in-out';

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

  public readonly buttonClasses = computed(() => `${PILL_BASE} ${this.active() ? PILL_ACTIVE : PILL_IDLE}`);

  public readonly iconClasses = computed(() =>
    [ICON_BASE, this.active() ? 'bg-white' : 'bg-gray-500', this.loading() ? 'animate-spin' : ''].join(' ').trim(),
  );

  public readonly labelClasses = computed(
    () => `toggle-label text-sm font-semibold whitespace-nowrap transition-colors duration-200 ease-in-out
      ${this.active() ? 'text-white' : 'text-gray-600'}`,
  );

  public readonly iconMask = computed(() => `url(/assets/icons/${this.loading() ? 'spinner' : this.icon()}.svg)`);
}
