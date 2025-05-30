import { Component, Input } from '@angular/core';
import { Issue } from '../../../../services/issue.service';
import { IssueTreeBranchComponent } from '../../../../components/issue-tree-branch/issue-tree-branch.component';

@Component({
  selector: 'app-release-important-issues',
  imports: [IssueTreeBranchComponent],
  templateUrl: './release-important-issues.component.html',
  styleUrl: './release-important-issues.component.scss',
})
export class ReleaseImportantIssuesComponent {
  @Input() releaseIssues?: Issue[] = [];
}
