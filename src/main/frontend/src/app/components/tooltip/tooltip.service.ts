import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Issue } from '../../services/issue.service';
import { SkipNode } from '../../pages/release-graph/release-link.service';

export interface TooltipData {
  issue?: Issue;
  skipNode?: SkipNode;
  top: string;
  left: string;
  isReleaseGraph?: boolean; // Flag to indicate release graph context
}

@Injectable({
  providedIn: 'root',
})
export class TooltipService {
  public tooltipSubject = new BehaviorSubject<TooltipData | null>(null);
  public readonly tooltipState$: Observable<TooltipData | null> = this.tooltipSubject.asObservable();

  public show(hostElement: HTMLElement, data: Issue | SkipNode, isReleaseGraph: boolean = false): void {
    const position = this.calculatePosition(hostElement, isReleaseGraph);
    if ('title' in data) {
      // It's an Issue
      this.tooltipSubject.next({ issue: data, ...position, isReleaseGraph });
    } else {
      // It's a SkipNode
      this.tooltipSubject.next({ skipNode: data, ...position, isReleaseGraph });
    }
  }

  public hide(): void {
    this.tooltipSubject.next(null);
  }

  private calculatePosition(host: HTMLElement, isReleaseGraph: boolean = false): { top: string; left: string } {
    const hostRect = host.getBoundingClientRect();
    const gap = 8;

    let top: number;
    if (isReleaseGraph) {
      // Position below the node for release graph
      top = hostRect.bottom + gap;
    } else {
      // Position above the node for other contexts (original behavior)
      top = hostRect.top - gap;
    }

    const left = hostRect.left + hostRect.width / 2;

    return { top: `${top}px`, left: `${left}px` };
  }
}
