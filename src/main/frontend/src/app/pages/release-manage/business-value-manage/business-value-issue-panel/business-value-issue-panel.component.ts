import { ChangeDetectionStrategy, Component, Signal, computed, input, output, signal } from '@angular/core';
import { BusinessValue } from '../../../../services/business-value.service';
import { Issue } from '../../../../services/issue.service';

export interface IssueWithSelection extends Issue {
  isSelected: boolean;
  isConnected: boolean;
  assignedToOther?: boolean;
  assignedBusinessValueTitle?: string;
}

@Component({
  selector: 'app-business-value-issue-panel',
  standalone: true,
  imports: [],
  templateUrl: './business-value-issue-panel.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './business-value-issue-panel.component.scss',
})
export class BusinessValueIssuePanelComponent {
  public readonly selectedBusinessValue = input.required<BusinessValue | null>();
  public readonly issuesWithSelection = input.required<IssueWithSelection[]>();
  public readonly hasChanges = input.required<boolean>();
  public readonly isSaving = input<boolean>(false);

  public readonly issueToggled = output<IssueWithSelection>();
  public readonly saveClicked = output<void>();

  public readonly issueSearchQuery = signal<string>('');

  public readonly sortedIssues: Signal<IssueWithSelection[]> = computed(() => {
    const issues = [...this.issuesWithSelection()];
    const selectedBV = this.selectedBusinessValue();
    const searchQuery = this.issueSearchQuery().toLowerCase().trim();

    let filteredIssues = issues;
    if (searchQuery) {
      filteredIssues = issues.filter(
        (issue) => issue.title.toLowerCase().includes(searchQuery) || issue.number.toString().includes(searchQuery),
      );
    }

    return [...filteredIssues].toSorted((issueA, issueB) =>
      BusinessValueIssuePanelComponent.sortIssuesByPriority(issueA, issueB, selectedBV),
    );
  });

  private static sortIssuesByPriority(
    issueA: IssueWithSelection,
    issueB: IssueWithSelection,
    selectedBusinessValue: BusinessValue | null,
  ): number {
    if (!selectedBusinessValue) return issueA.number - issueB.number;

    if (issueA.isConnected && !issueB.isConnected) return -1;
    if (!issueA.isConnected && issueB.isConnected) return 1;

    const isAFree = !issueA.isConnected && !issueA.assignedToOther;
    const isBFree = !issueB.isConnected && !issueB.assignedToOther;
    if (isAFree && !isBFree) return -1;
    if (!isAFree && isBFree) return 1;

    return issueA.number - issueB.number;
  }
  public updateSearchQuery(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.issueSearchQuery.set(query);
  }

  public toggleIssue(issue: IssueWithSelection): void {
    this.issueToggled.emit(issue);
  }

  public onSaveClick(): void {
    this.saveClicked.emit();
  }
}
