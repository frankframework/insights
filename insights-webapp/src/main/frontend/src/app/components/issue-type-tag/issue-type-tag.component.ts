import { Component, inject, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ColorService } from '../../services/color.service';

interface IssueType {
  name: string;
  color: string;
}

@Component({
  selector: 'app-issue-type-tag',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './issue-type-tag.component.html',
  styleUrls: ['./issue-type-tag.component.scss'],
})
export class IssueTypeTagComponent {
  @Input() issueType!: IssueType;

  private colorService = inject(ColorService);

  public getIssueTypeColor(issueTypeColor: string): string {
    return this.colorService.getTypeTextColor(issueTypeColor);
  }
}
