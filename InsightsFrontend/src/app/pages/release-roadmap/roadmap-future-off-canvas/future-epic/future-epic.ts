import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Issue } from '../../../../services/issue.service';
import { IssueTypeTagComponent } from '../../../../components/issue-type-tag/issue-type-tag.component';

@Component({
  selector: 'app-future-epic',
  standalone: true,
  imports: [CommonModule, IssueTypeTagComponent],
  templateUrl: './future-epic.html',
  styleUrl: './future-epic.scss',
})
export class FutureEpic {
  @Input() futureEpicIssue!: Issue;
}
