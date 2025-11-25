import { Component, Input, computed, signal, OnChanges, SimpleChanges, inject } from '@angular/core';
import { Issue } from '../../../services/issue.service';
import { IssueTreeBranchComponent } from './issue-tree-branch/issue-tree-branch.component';
import { FormsModule } from '@angular/forms';
import { BusinessValue, BusinessValueService } from '../../../services/business-value.service';
import { catchError, of } from 'rxjs';

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
  @Input() releaseId?: string;

  public selectedType = signal<string | null | 'all'>('all');
  public showBusinessValues = signal<boolean>(false);
  public businessValues = signal<BusinessValue[]>([]);
  public isLoadingBusinessValues = signal<boolean>(false);

  private businessValueService = inject(BusinessValueService);

  public issueTypeOptions = computed<IssueTypeOption[]>(() => {
    const issues = this.issuesSignal();

    const typeNames = issues.map((issue) => this.getIssueTypeName(issue));
    const uniqueTypeNames = [...new Set(typeNames)];
    uniqueTypeNames.sort((a, b) => this.sortIssueTypeNames(a, b));

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

  // eslint-disable-next-line unicorn/consistent-function-scoping
  public sortedAndFilteredIssues = computed(() => {
    const issues = this.issuesSignal();
    if (!issues) return [];
    let filtered = [...issues];

    if (this.selectedType() !== 'all') {
      const filterPredicate = this.selectedType() === null ? this.isIssueWithoutType : this.isIssueWithSelectedType;

      filtered = filtered.filter((issue) => filterPredicate(issue));
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
    const aGroup = this.groupOrder(a);
    const bGroup = this.groupOrder(b);

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

  private groupOrder(issue: Issue): number {
    if ((issue.issueType?.name ?? '').toUpperCase() === 'EPIC') return 0;
    if (issue.subIssues && issue.subIssues.length > 0) return 1;
    return 2;
  }

  private isIssueWithoutType(issue: Issue): boolean {
    return !issue.issueType?.name;
  }

  private isIssueWithSelectedType = (issue: Issue): boolean => {
    return issue.issueType?.name === this.selectedType();
  };

  private getIssueTypeName(issue: Issue): string | null {
    return issue.issueType?.name ?? null;
  }

  private sortIssueTypeNames = (a: string | null, b: string | null): number => {
    const aPriority = this.getPriority(a);
    const bPriority = this.getPriority(b);
    if (aPriority !== bPriority) return aPriority - bPriority;
    return (a ?? '').localeCompare(b ?? '');
  };

  private getPriority(typeName?: string | null): number {
    const name = (typeName ?? '').toUpperCase() as TypePriorityKey;
    if (name in TypePriority) return TypePriority[name];
    if (!typeName) return TypePriority.NONE;
    return Number.MAX_SAFE_INTEGER;
  }

  public toggleBusinessValues(): void {
    const newValue = !this.showBusinessValues();
    this.showBusinessValues.set(newValue);

    if (newValue && this.releaseId && this.businessValues().length === 0) {
      this.fetchBusinessValues();
    }
  }

  private fetchBusinessValues(): void {
    if (!this.releaseId) return;

    this.isLoadingBusinessValues.set(true);
    this.businessValueService
      .getBusinessValuesByReleaseId(this.releaseId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load business values:', error);
          return of([]);
        }),
      )
      .subscribe((businessValues) => {
        this.businessValues.set(businessValues);
        this.isLoadingBusinessValues.set(false);
      });
  }
}
