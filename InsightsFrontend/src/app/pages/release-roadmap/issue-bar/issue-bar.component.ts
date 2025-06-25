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

  ngOnInit(): void {
    this.isClosed = this.issue.state === GitHubStates.CLOSED;
    this.setPriorityStyle();
  }

  private setPriorityStyle(): void {
    if (this.isClosed) {
      this.priorityStyle = {
        'background-color': '#f3e8ff',
        color: '#581c87',
        'border-color': '#d8b4fe',
      };
      return;
    }
    const color = this.issue.issuePriority?.color;
    const defaultStyles = { 'background-color': '#e5e7eb', color: '#4b5563', 'border-color': '#d1d5db' };
    this.priorityStyle =
      color && /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/.test(color)
        ? {
            'background-color': `#${color}25`,
            color: `#${color}`,
            'border-color': `#${color}`,
          }
        : defaultStyles;
  }
}
