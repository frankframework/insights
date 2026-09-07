import { Component, input, output } from '@angular/core';
import { ModalComponent } from '../../../components/modal/modal.component';
import { MarkdownPipe } from '../../../pipes/markdown.pipe';
import { BusinessValue } from '../../../services/business-value.service';
import { Issue } from '../../../services/issue.service';
import { IssueTreeBranchComponent } from '../release-important-issues/issue-tree-branch/issue-tree-branch.component';

@Component({
  selector: 'app-release-business-value-modal',
  standalone: true,
  imports: [ModalComponent, IssueTreeBranchComponent, MarkdownPipe],
  templateUrl: './release-business-value-modal.component.html',
})
export class ReleaseBusinessValueModalComponent {
  public readonly businessValue = input.required<BusinessValue | null>();
  public readonly closed = output<void>();

  public close(): void {
    this.closed.emit();
  }

  public openIssue(issue: Issue): void {
    if (issue.url) {
      window.open(issue.url, '_blank');
    }
  }
}
