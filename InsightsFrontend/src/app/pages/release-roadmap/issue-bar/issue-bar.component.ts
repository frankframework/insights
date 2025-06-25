import { Component, Input, OnInit } from '@angular/core';
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

  ngOnInit(): void {
    this.isClosed = this.issue.state === GitHubStates.CLOSED;
    this.priorityStyle = this.getStyleForState();
  }

  private getStyleForState(): Record<string, string> {
    if (this.isClosed) {
      return this.CLOSED_STYLE;
    }

    const priorityColor = this.issue.issuePriority?.color;
    if (this.isValidHexColor(priorityColor)) {
      return this.getPriorityStyles(priorityColor!);
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
}
