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

  private calculatePosition(host: HTMLElement): { top: string; left: string } {
    const hostRect = host.getBoundingClientRect();
    const gap = 8;

    const top = hostRect.top - gap;
    const left = hostRect.left + hostRect.width / 2;

    return { top: `${top}px`, left: `${left}px` };
  }
}
