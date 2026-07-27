import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

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
  public tooltipSubject = new BehaviorSubject<TooltipData | null>(null);
  public readonly tooltipState$: Observable<TooltipData | null> = this.tooltipSubject.asObservable();

  public show(hostElement: HTMLElement, title: string, details: TooltipDetail[] = []): void {
    const position = this.calculatePosition(hostElement);
    this.tooltipSubject.next({ title, details, ...position });
  }

  public hide(): void {
    this.tooltipSubject.next(null);
  }

  private calculatePosition(host: HTMLElement): { top: string; left: string } {
    const hostRect = host.getBoundingClientRect();
    const gap = 8;

    const top = hostRect.top - gap;
    const left = hostRect.left + hostRect.width / 2;

    return { top: `${top}px`, left: `${left}px` };
  }
}
