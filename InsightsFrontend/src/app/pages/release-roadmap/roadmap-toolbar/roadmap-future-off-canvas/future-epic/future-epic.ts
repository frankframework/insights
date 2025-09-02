import { Component, Input } from '@angular/core';
import { Issue } from '../../../../../services/issue.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-future-epic',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './future-epic.html',
  styleUrl: './future-epic.scss',
})
export class FutureEpic {
  @Input() futureEpicIssue!: Issue;
}
