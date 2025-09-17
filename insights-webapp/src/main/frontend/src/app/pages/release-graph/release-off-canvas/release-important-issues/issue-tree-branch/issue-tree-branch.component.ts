import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Issue } from '../../../../../services/issue.service';
import { IssueTypeTagComponent } from '../../../../../components/issue-type-tag/issue-type-tag.component';

@Component({
  selector: 'app-issue-tree-branch',
  standalone: true,
  imports: [CommonModule, IssueTypeTagComponent],
  templateUrl: './issue-tree-branch.component.html',
  styleUrl: './issue-tree-branch.component.scss',
})
export class IssueTreeBranchComponent {
  private static readonly MAX_SUB_ISSUE_DEPTH = 8;

  @Input() issue!: Issue;
  @Input() depth = 0;

  protected expanded = false;

  public toggleExpand(): void {
    this.expanded = !this.expanded;
  }

  public getIndent(): string {
    const d = Math.min(this.depth, IssueTreeBranchComponent.MAX_SUB_ISSUE_DEPTH);
    return `${d}rem`;
  }
}
