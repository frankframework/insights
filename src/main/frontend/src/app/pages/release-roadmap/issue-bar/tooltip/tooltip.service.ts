import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Issue } from '../../../../services/issue.service';

export interface TooltipData {
  issue: Issue;
  top: string;
  left: string;
}

@Injectable({
  providedIn: 'root',
})
export class TooltipService {
  public tooltipSubject = new BehaviorSubject<TooltipData | null>(null);
  public readonly tooltipState$: Observable<TooltipData | null> = this.tooltipSubject.asObservable();

  public show(hostElement: HTMLElement, issue: Issue): void {
    const position = this.calculatePosition(hostElement);
    this.tooltipSubject.next({ issue, ...position });
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
