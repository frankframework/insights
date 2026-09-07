import { Injectable, Signal, WritableSignal, signal } from '@angular/core';

export interface TooltipDetail {
  label?: string;
  value: string;
}

export interface TooltipData {
  title: string;
  details: TooltipDetail[];
  top: string;
  left: string;
  placement: 'above' | 'below';
}

@Injectable({
  providedIn: 'root',
})
export class TooltipService {
  public readonly tooltip: Signal<TooltipData | null>;

  private readonly tooltipState: WritableSignal<TooltipData | null> = signal<TooltipData | null>(null);

  constructor() {
    this.tooltip = this.tooltipState.asReadonly();
  }

  public show(hostElement: HTMLElement, title: string, details: TooltipDetail[] = []): void {
    const position = this.calculatePosition(hostElement);
    this.tooltipState.set({ title, details, ...position });
  }

  public hide(): void {
    this.tooltipState.set(null);
  }

  private calculatePosition(host: HTMLElement): { top: string; left: string; placement: 'above' | 'below' } {
    const hostRect = host.getBoundingClientRect();
    const gap = 8;
    const estimatedTooltipHeight = 120;
    const tooltipWidth = Math.min(300, window.innerWidth / 2);

    const placement: 'above' | 'below' = hostRect.top - gap - estimatedTooltipHeight < 0 ? 'below' : 'above';
    const top = placement === 'above' ? hostRect.top - gap : hostRect.bottom + gap;

    const halfWidth = tooltipWidth / 2;
    const rawLeft = hostRect.left + hostRect.width / 2;
    const left = Math.min(Math.max(rawLeft, halfWidth + gap), window.innerWidth - halfWidth - gap);

    return { top: `${top}px`, left: `${left}px`, placement };
  }
}
