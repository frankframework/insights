import { ChangeDetectionStrategy, Component, Signal, computed, inject, input } from '@angular/core';
import { ColorService } from '../../services/color.service';

interface IssueType {
  name: string;
  color: string;
}

@Component({
  selector: 'app-issue-type-tag',
  standalone: true,
  imports: [],
  templateUrl: './issue-type-tag.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./issue-type-tag.component.scss'],
})
export class IssueTypeTagComponent {
  public readonly issueType = input.required<IssueType>();
  public readonly textColor: Signal<string>;

  private readonly colorService = inject(ColorService);

  constructor() {
    this.textColor = computed(() => this.colorService.getTypeTextColor(this.issueType().color));
  }
}
