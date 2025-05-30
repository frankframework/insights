import { Component, Input } from '@angular/core';
import { Issue } from '../../services/issue.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-issue-tree-branch',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './issue-tree-branch.component.html',
  styleUrl: './issue-tree-branch.component.scss',
})
export class IssueTreeBranchComponent {
  private static readonly MAX_SUB_ISSUE_DEPTH = 8;

  @Input() issue!: Issue;
  @Input() depth = 0;

  public getIndent(): string {
    const d = Math.min(this.depth, IssueTreeBranchComponent.MAX_SUB_ISSUE_DEPTH);
    return `${d}rem`;
  }
}
