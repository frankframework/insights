import { Component, input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  imports: [CommonModule],
  templateUrl: './business-value-issue-panel.component.html',
  styleUrl: './business-value-issue-panel.component.scss',
})
export class BusinessValueIssuePanelComponent {
  public selectedBusinessValue = input.required<BusinessValue | null>();
  public issuesWithSelection = input.required<IssueWithSelection[]>();
  public hasChanges = input.required<boolean>();
  public isSaving = input<boolean>(false);

  @Output() issueToggled = new EventEmitter<IssueWithSelection>();
  @Output() saveClicked = new EventEmitter<void>();

  public issueSearchQuery = signal<string>('');

  public sortedIssues = computed(() => {
    const issues = [...this.issuesWithSelection()];
    const selectedBV = this.selectedBusinessValue();
    const searchQuery = this.issueSearchQuery().toLowerCase().trim();

    let filteredIssues = issues;
    if (searchQuery) {
      filteredIssues = issues.filter(
        (issue) => issue.title.toLowerCase().includes(searchQuery) || issue.number.toString().includes(searchQuery),
      );
    }

    return [...filteredIssues].toSorted((a, b) => this.sortIssuesByPriority(a, b, selectedBV));
  });

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

  private sortIssuesByPriority(a: IssueWithSelection, b: IssueWithSelection, selectedBV: BusinessValue | null): number {
    if (!selectedBV) return a.number - b.number;

    if (a.isConnected && !b.isConnected) return -1;
    if (!a.isConnected && b.isConnected) return 1;

    const aFree = !a.isConnected && !a.assignedToOther;
    const bFree = !b.isConnected && !b.assignedToOther;
    if (aFree && !bFree) return -1;
    if (!aFree && bFree) return 1;

    return a.number - b.number;
  }
}
