import { Component, Input, computed, signal, OnChanges, SimpleChanges } from '@angular/core';
import { Issue } from '../../../../services/issue.service';
import { IssueTreeBranchComponent } from '../../../../components/issue-tree-branch/issue-tree-branch.component';
import { FormsModule } from '@angular/forms';

export const TypePriority = {
  EPIC: 0,
  FEATURE: 1,
  BUG: 2,
  TASK: 3,
  NONE: 4,
} as const;

type TypePriorityKey = keyof typeof TypePriority;

interface IssueTypeOption {
  label: string;
  value: string | null;
}

@Component({
  selector: 'app-release-important-issues',
  imports: [IssueTreeBranchComponent, FormsModule],
  templateUrl: './release-important-issues.component.html',
  styleUrl: './release-important-issues.component.scss',
})
export class ReleaseImportantIssuesComponent implements OnChanges {
  @Input() releaseIssues?: Issue[] = [];

  public selectedType = signal<string | null | 'all'>('all');

  public issueTypeOptions = computed<IssueTypeOption[]>(() => {
    const issues = this.issuesSignal();
    const typeNames = issues.map((issue) => issue.issueType?.name ?? null);
    const uniqueTypeNames = [...new Set(typeNames)];
    uniqueTypeNames.sort((a, b) => {
      const aPriority = this.getPriority(a);
      const bPriority = this.getPriority(b);
      if (aPriority !== bPriority) return aPriority - bPriority;
      return (a ?? '').localeCompare(b ?? '');
    });
    const options: IssueTypeOption[] = [{ label: 'All types', value: 'all' }];
    for (const name of uniqueTypeNames) {
      if (name) {
        options.push({ label: name, value: name });
      } else {
        options.push({ label: '(No type)', value: null });
      }
    }
    return options;
  });

  public sortedAndFilteredIssues = computed(() => {
    const issues = this.issuesSignal();
    if (!issues) return [];
    let filtered = [...issues];
    if (this.selectedType() !== 'all') {
      filtered =
        this.selectedType() === null
          ? filtered.filter((issue) => !issue.issueType?.name)
          : filtered.filter((issue) => issue.issueType?.name === this.selectedType());
    }
    filtered.sort(this.sortIssues);
    return filtered;
  });

  private issuesSignal = signal<Issue[]>([]);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['releaseIssues']) {
      this.selectedType.set('all');
      this.issuesSignal.set(this.releaseIssues ? [...this.releaseIssues] : []);
    }
  }

  private sortIssues = (a: Issue, b: Issue): number => {
    const groupOrder = (issue: Issue): number => {
      if ((issue.issueType?.name ?? '').toUpperCase() === 'EPIC') return 0;
      if (issue.subIssues && issue.subIssues.length > 0) return 1;
      return 2;
    };
    const aGroup = groupOrder(a);
    const bGroup = groupOrder(b);
    if (aGroup !== bGroup) return aGroup - bGroup;
    if (aGroup === 2) {
      const aPriority = this.getPriority(a.issueType?.name);
      const bPriority = this.getPriority(b.issueType?.name);
      if (aPriority !== bPriority) return aPriority - bPriority;
      const cmp = (a.issueType?.name ?? '').localeCompare(b.issueType?.name ?? '');
      if (cmp !== 0) return cmp;
      return a.number - b.number;
    }
    return a.number - b.number;
  };

  private getPriority(typeName?: string | null): number {
    const name = (typeName ?? '').toUpperCase() as TypePriorityKey;
    if (name in TypePriority) return TypePriority[name];
    if (!typeName) return TypePriority.NONE;
    return Number.MAX_SAFE_INTEGER;
  }
}
