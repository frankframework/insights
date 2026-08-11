import { Component, ElementRef, Signal, computed, inject, input, viewChild } from '@angular/core';
import { NgStyle } from '@angular/common';
import { GitHubStates } from '../../../app.service';
import { Issue } from '../../../services/issue.service';
import { TooltipDetail, TooltipService } from '../../../components/tooltip/tooltip.service';
import { ISSUE_STATE_STYLES, CLOSED_STYLE, OPEN_STYLE, ViewMode } from '../roadmap.types';

@Component({
  selector: 'app-issue-bar',
  standalone: true,
  imports: [NgStyle],
  templateUrl: './issue-bar.component.html',
  styleUrls: ['./issue-bar.component.scss'],
})
export class IssueBarComponent {
  public readonly issue = input.required<Issue>();
  public readonly issueStyle = input<Record<string, string>>({});
  public readonly isUnplannedEpic = input(false);
  public readonly viewMode = input<ViewMode>(ViewMode.QUARTERLY);

  public readonly issueLinkRef = viewChild.required<ElementRef<HTMLAnchorElement>>('issueLink');

  public ViewMode = ViewMode;

  public readonly isClosed: Signal<boolean> = computed(() => this.issue().state === GitHubStates.CLOSED);
  public readonly priorityStyle: Signal<Record<string, string>> = computed(() => this.getStyleForState());

  private tooltipService = inject(TooltipService);
  private readonly CLOSED_STYLE = CLOSED_STYLE;
  private readonly OPEN_STYLE = OPEN_STYLE;
  private readonly ISSUE_STATE_STYLES = ISSUE_STATE_STYLES;

  public onMouseEnter(): void {
    if (this.viewMode() === ViewMode.MONTHLY) {
      return;
    }

    const issueLinkReference = this.issueLinkRef();
    if (issueLinkReference) {
      const issue = this.issue();
      const details: TooltipDetail[] = [];
      if (issue.issuePriority) {
        details.push({ label: 'Priority', value: issue.issuePriority.name });
      }
      if (issue.points) {
        details.push({ label: 'Points', value: `${issue.points}` });
      }

      this.tooltipService.show(issueLinkReference.nativeElement, issue.title, details);
    }
  }

  public onMouseLeave(): void {
    if (this.viewMode() === ViewMode.MONTHLY) {
      return;
    }

    this.tooltipService.hide();
  }

  private getStyleForState(): Record<string, string> {
    const issue = this.issue();
    const isEpic = issue.issueType?.name === 'Epic';

    if (isEpic && issue.subIssues && issue.subIssues.length > 0) {
      return this.getEpicGradientStyle();
    }

    const issueStateName = issue.issueState?.name;
    if (issueStateName && this.ISSUE_STATE_STYLES[issueStateName]) {
      return this.ISSUE_STATE_STYLES[issueStateName];
    }

    if (this.isClosed()) {
      return this.CLOSED_STYLE;
    }

    const priorityColor = issue.issuePriority?.color;
    if (this.isValidHexColor(priorityColor)) {
      return this.getPriorityStyles(priorityColor);
    }

    return this.OPEN_STYLE;
  }

  private getPriorityStyles(color: string): Record<string, string> {
    return {
      'background-color': `#${color}25`,
      color: `#${color}`,
      'border-color': `#${color}`,
    };
  }

  private isValidHexColor(color: string | undefined | null): color is string {
    if (!color) {
      return false;
    }
    return /^([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(color);
  }

  private getEpicGradientStyle(): Record<string, string> {
    const stateDistribution = this.getSubIssueStateDistribution();
    const totalSubIssues = this.issue().subIssues!.length;

    const backgroundGradientStops: string[] = [];
    const borderGradientStops: string[] = [];
    let currentPosition = 0;

    const sortedStates = [...stateDistribution.entries()].toSorted((a, b) => {
      const order = ['Todo', 'On hold', 'In Progress', 'Review', 'Done', 'closed', 'open'];
      const indexA = order.indexOf(a[0]);
      const indexB = order.indexOf(b[0]);
      return (indexA === -1 ? 999 : indexA) - (indexB === -1 ? 999 : indexB);
    });

    for (const [stateName, count] of sortedStates) {
      const percentage = (count / totalSubIssues) * 100;
      const backgroundColor = this.getBackgroundColorForState(stateName);
      const borderColor = this.getBorderColorForState(stateName);

      if (currentPosition === 0 && percentage === 100) {
        backgroundGradientStops.push(backgroundColor);
        borderGradientStops.push(borderColor);
      } else {
        backgroundGradientStops.push(`${backgroundColor} ${currentPosition}%`);
        borderGradientStops.push(`${borderColor} ${currentPosition}%`);
        currentPosition += percentage;
        backgroundGradientStops.push(`${backgroundColor} ${currentPosition}%`);
        borderGradientStops.push(`${borderColor} ${currentPosition}%`);
      }
    }

    const backgroundGradient = `linear-gradient(to right, ${backgroundGradientStops.join(', ')})`;
    const borderGradient = `linear-gradient(to right, ${borderGradientStops.join(', ')})`;

    const dominantState = sortedStates.reduce((a, b) => (a[1] > b[1] ? a : b))[0];
    const textColor = this.getTextColorForState(dominantState);

    const combinedBackground = `${backgroundGradient} padding-box, ${borderGradient} border-box`;

    return {
      background: combinedBackground,
      border: '1px solid transparent',
      color: textColor,
    };
  }

  private getSubIssueStateDistribution(): Map<string, number> {
    const distribution = new Map<string, number>();

    for (const subIssue of this.issue().subIssues!) {
      let stateName: string;

      if (subIssue.issueState?.name && this.ISSUE_STATE_STYLES[subIssue.issueState.name]) {
        stateName = subIssue.issueState.name;
      } else if (subIssue.state === GitHubStates.CLOSED) {
        stateName = 'closed';
      } else {
        stateName = 'open';
      }

      distribution.set(stateName, (distribution.get(stateName) || 0) + 1);
    }

    return distribution;
  }

  private getBackgroundColorForState(stateName: string): string {
    if (this.ISSUE_STATE_STYLES[stateName]) {
      return this.ISSUE_STATE_STYLES[stateName]['background-color'];
    } else if (stateName === 'closed') {
      return this.CLOSED_STYLE['background-color'];
    } else {
      return this.OPEN_STYLE['background-color'];
    }
  }

  private getTextColorForState(stateName: string): string {
    if (this.ISSUE_STATE_STYLES[stateName]) {
      return this.ISSUE_STATE_STYLES[stateName]['color'];
    } else if (stateName === 'closed') {
      return this.CLOSED_STYLE['color'];
    } else {
      return this.OPEN_STYLE['color'];
    }
  }

  private getBorderColorForState(stateName: string): string {
    if (this.ISSUE_STATE_STYLES[stateName]) {
      return this.ISSUE_STATE_STYLES[stateName]['border-color'];
    } else if (stateName === 'closed') {
      return this.CLOSED_STYLE['border-color'];
    } else {
      return this.OPEN_STYLE['border-color'];
    }
  }
}
