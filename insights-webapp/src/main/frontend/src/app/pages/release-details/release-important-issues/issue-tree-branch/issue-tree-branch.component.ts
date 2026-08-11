import { Component, Signal, computed, input, signal } from '@angular/core';
import { Issue } from '../../../../services/issue.service';
import { IssueTypeTagComponent } from '../../../../components/issue-type-tag/issue-type-tag.component';

@Component({
  selector: 'app-issue-tree-branch',
  standalone: true,
  imports: [IssueTypeTagComponent],
  templateUrl: './issue-tree-branch.component.html',
  styleUrl: './issue-tree-branch.component.scss',
})
export class IssueTreeBranchComponent {
  private static readonly MAX_SUB_ISSUE_DEPTH = 8;

  public readonly issue = input.required<Issue>();
  public readonly depth = input(0);

  public readonly indent: Signal<string> = computed(() => {
    const depth = Math.min(this.depth(), IssueTreeBranchComponent.MAX_SUB_ISSUE_DEPTH);
    return `${depth * 2}rem`;
  });

  protected readonly expanded = signal(false);

  public toggleExpand(event?: MouseEvent): void {
    event?.preventDefault();
    this.expanded.update((isExpanded) => !isExpanded);
  }
}
