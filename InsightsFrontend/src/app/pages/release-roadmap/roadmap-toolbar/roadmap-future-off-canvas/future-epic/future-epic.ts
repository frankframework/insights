import { Component, Input } from '@angular/core';
import { Issue } from '../../../../../services/issue.service';

@Component({
  selector: 'app-future-epic',
  imports: [],
  templateUrl: './future-epic.html',
  styleUrl: './future-epic.scss',
})
export class FutureEpic {
  @Input() futureEpicIssue!: Issue;
}
