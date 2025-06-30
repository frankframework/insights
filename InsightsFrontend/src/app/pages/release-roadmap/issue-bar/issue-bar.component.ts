import { Component, ElementRef, Input, OnInit, Renderer2, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GitHubStates } from '../../../app.service';
import { Issue } from '../../../services/issue.service';

@Component({
  selector: 'app-issue-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './issue-bar.component.html',
  styleUrls: ['./issue-bar.component.scss'],
})
export class IssueBarComponent implements OnInit {
  @Input({ required: true }) issue!: Issue;
  @Input() issueStyle: Record<string, string> = {};

  @ViewChild('tooltip') tooltipRef!: ElementRef<HTMLDivElement>;
  @ViewChild('issueLink') issueLinkRef!: ElementRef<HTMLAnchorElement>;

  public priorityStyle: Record<string, string> = {};
  public isClosed = false;

  private readonly CLOSED_STYLE: Record<string, string> = {
    'background-color': '#f3e8ff',
    color: '#581c87',
    'border-color': '#d8b4fe',
  };

  private readonly OPEN_STYLE: Record<string, string> = {
    'background-color': '#f0fdf4',
    color: '#166534',
    'border-color': '#86efac',
  };

  constructor(
    private renderer: Renderer2,
    private element: ElementRef<HTMLElement>,
  ) {}

  ngOnInit(): void {
    this.isClosed = this.issue.state === GitHubStates.CLOSED;
    this.priorityStyle = this.getStyleForState();
  }

  public showTooltip(): void {
    const tooltipElement = this.tooltipRef.nativeElement;
    // FIX: Use the specific, styled anchor tag to measure position from.
    const elementToMeasure = this.issueLinkRef.nativeElement;

    this.renderer.appendChild(document.body, tooltipElement);
    this.renderer.setStyle(tooltipElement, 'display', 'block');

    setTimeout(() => {
      this.positionTooltip(tooltipElement, elementToMeasure);
    }, 0);
  }

  public hideTooltip(): void {
    const tooltipElement = this.tooltipRef.nativeElement;
    this.renderer.setStyle(tooltipElement, 'display', 'none');
    this.renderer.appendChild(this.element.nativeElement, tooltipElement);
  }

  private positionTooltip(tooltip: HTMLElement, host: HTMLElement): void {
    const hostRect = host.getBoundingClientRect();
    const tooltipRect = tooltip.getBoundingClientRect();
    const viewportWidth = window.innerWidth;
    const gap = 8;

    let top = hostRect.top - tooltipRect.height - gap;
    let left = hostRect.left + hostRect.width / 2 - tooltipRect.width / 2;

    if (top < gap) {
      top = hostRect.bottom + gap;
    }
    if (left < gap) {
      left = gap;
    }
    if (left + tooltipRect.width > viewportWidth - gap) {
      left = viewportWidth - tooltipRect.width - gap;
    }

    this.renderer.setStyle(tooltip, 'position', 'fixed');
    this.renderer.setStyle(tooltip, 'top', `${top}px`);
    this.renderer.setStyle(tooltip, 'left', `${left}px`);
  }

  private getStyleForState(): Record<string, string> {
    if (this.isClosed) return this.CLOSED_STYLE;
    const priorityColor = this.issue.issuePriority?.color;
    if (this.isValidHexColor(priorityColor)) return this.getPriorityStyles(priorityColor);
    return this.OPEN_STYLE;
  }

  private getPriorityStyles(color: string): Record<string, string> {
    return { 'background-color': `#${color}25`, color: `#${color}`, 'border-color': `#${color}` };
  }

  private isValidHexColor(color: string | undefined | null): color is string {
    if (!color) return false;
    return /^([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(color);
  }
}
