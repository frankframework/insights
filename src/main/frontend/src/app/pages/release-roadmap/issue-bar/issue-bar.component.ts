import { Component, Input, OnInit, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GitHubStates } from '../../../app.service';
import { Issue } from '../../../services/issue.service';
import { TooltipService } from './tooltip/tooltip.service';

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

  @ViewChild('issueLink') issueLinkRef!: ElementRef<HTMLAnchorElement>;

  public priorityStyle: Record<string, string> = {};
  public isClosed = false;

  private tooltipService = inject(TooltipService);

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

  private readonly ISSUE_STATE_STYLES: Record<string, Record<string, string>> = {
    Todo: {
      'background-color': '#fefce8',
      color: '#a16207',
      'border-color': '#fde047',
    },
    'On hold': {
      'background-color': '#fee2e2',
      color: '#991b1b',
      'border-color': '#fca5a5',
    },
    'In Progress': {
      'background-color': '#dbeafe',
      color: '#1e3a8a',
      'border-color': '#93c5fd',
    },
    Review: {
      'background-color': '#dcfce7',
      color: '#166534',
      'border-color': '#86efac',
    },
    Done: {
      'background-color': '#e5e7eb',
      color: '#4b5563',
      'border-color': '#d1d5db',
    },
  };

  ngOnInit(): void {
    this.isClosed = this.issue.state === GitHubStates.CLOSED;
    this.priorityStyle = this.getStyleForState();
  }

  public onMouseEnter(): void {
    if (this.issueLinkRef) {
      this.tooltipService.show(this.issueLinkRef.nativeElement, this.issue);
    }
  }

  public onMouseLeave(): void {
    this.tooltipService.hide();
  }

  private getStyleForState(): Record<string, string> {
    if (this.issue.subIssues && this.issue.subIssues.length > 0) {
      return this.getEpicGradientStyle();
    }

    const issueStateName = this.issue.issueState?.name;
    if (issueStateName && this.ISSUE_STATE_STYLES[issueStateName]) {
      return this.ISSUE_STATE_STYLES[issueStateName];
    }

    if (this.isClosed) {
      return this.CLOSED_STYLE;
    }

    const priorityColor = this.issue.issuePriority?.color;
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
    const totalSubIssues = this.issue.subIssues!.length;

    const backgroundGradientStops: string[] = [];
    const borderGradientStops: string[] = [];
    let currentPosition = 0;

    const sortedStates = [...stateDistribution.entries()].sort((a, b) => {
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

    for (const subIssue of this.issue.subIssues!) {
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
