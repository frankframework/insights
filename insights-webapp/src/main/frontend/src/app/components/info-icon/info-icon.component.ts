import { Component, inject, input } from '@angular/core';
import { TooltipService } from '../tooltip/tooltip.service';

@Component({
  selector: 'app-info-icon',
  standalone: true,
  template: `
    <svg viewBox="0 0 16 16" fill="none" aria-hidden="true" class="size-3">
      <circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5" />
      <circle cx="8" cy="4.75" r="0.9" fill="currentColor" />
      <path d="M8 7.25v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
    </svg>
  `,
  host: {
    class:
      'inline-flex cursor-help items-center justify-center text-gray-400 outline-none hover:text-gray-500 focus-visible:text-gray-500',
    tabindex: '0',
    role: 'img',
    '[attr.aria-label]': 'text()',
    '(mouseenter)': 'show($event)',
    '(mouseleave)': 'hide()',
    '(focus)': 'show($event)',
    '(blur)': 'hide()',
  },
})
export class InfoIconComponent {
  public readonly infoTitle = input.required<string>();
  public readonly text = input.required<string>();

  private readonly tooltipService = inject(TooltipService);

  public show(event: Event): void {
    this.tooltipService.show(event.currentTarget as HTMLElement, this.infoTitle(), [{ value: this.text() }]);
  }

  public hide(): void {
    this.tooltipService.hide();
  }
}

