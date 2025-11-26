import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { BusinessValue, BusinessValueService } from '../../../services/business-value.service';
import { Issue, IssueService } from '../../../services/issue.service';
import { catchError, of, forkJoin } from 'rxjs';

interface IssueWithSelection extends Issue {
  isSelected: boolean;
  isConnected: boolean;
}

@Component({
  selector: 'app-business-value-manage',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './business-value-manage.component.html',
  styleUrl: './business-value-manage.component.scss',
})
export class BusinessValueManageComponent implements OnInit {
  public businessValues = signal<BusinessValue[]>([]);
  public allIssues = signal<Issue[]>([]);
  public issuesWithSelection = signal<IssueWithSelection[]>([]);
  public selectedBusinessValue = signal<BusinessValue | null>(null);
  public isLoading = signal<boolean>(true);
  public isSaving = signal<boolean>(false);
  public releaseId = signal<string>('');

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private businessValueService = inject(BusinessValueService);
  private issueService = inject(IssueService);

  private originalSelectedIssueIds = new Set<string>();

  public hasChanges = computed(() => {
    const currentSelectedIds = new Set(
      this.issuesWithSelection()
        .filter((issue) => issue.isSelected)
        .map((issue) => issue.id)
    );

    if (currentSelectedIds.size !== this.originalSelectedIssueIds.size) {
      return true;
    }

    for (const id of currentSelectedIds) {
      if (!this.originalSelectedIssueIds.has(id)) {
        return true;
      }
    }

    return false;
  });

  public sortedIssues = computed(() => {
    const issues = [...this.issuesWithSelection()];
    const selectedBV = this.selectedBusinessValue();

    if (!selectedBV) return issues;

    return issues.sort((a, b) => {
      // Connected issues first
      if (a.isConnected && !b.isConnected) return -1;
      if (!a.isConnected && b.isConnected) return 1;

      // Then by selection
      if (a.isSelected && !b.isSelected) return -1;
      if (!a.isSelected && b.isSelected) return 1;

      // Then by number
      return a.number - b.number;
    });
  });

  ngOnInit(): void {
    const releaseId = this.route.snapshot.paramMap.get('id');
    if (releaseId) {
      this.releaseId.set(releaseId);
      this.fetchData(releaseId);
    }
  }

  public goBack(): void {
    const releaseId = this.releaseId();
    if (releaseId) {
      this.router.navigate(['/release-manage', releaseId]);
    }
  }

  public selectBusinessValue(businessValue: BusinessValue): void {
    this.selectedBusinessValue.set(businessValue);
    this.updateIssueSelection(businessValue);
  }

  public toggleIssue(issue: IssueWithSelection): void {
    const issues = this.issuesWithSelection();
    const updatedIssues = issues.map((i) =>
      i.id === issue.id ? { ...i, isSelected: !i.isSelected } : i
    );
    this.issuesWithSelection.set(updatedIssues);
  }

  public saveChanges(): void {
    const selectedBV = this.selectedBusinessValue();
    if (!selectedBV || !this.hasChanges()) return;

    this.isSaving.set(true);

    const selectedIssueIds = this.issuesWithSelection()
      .filter((issue) => issue.isSelected)
      .map((issue) => issue.id);

    this.businessValueService.updateIssueConnections(selectedBV.id, selectedIssueIds).subscribe({
      next: (updatedBV) => {
        // Update the original selection to match current
        this.originalSelectedIssueIds = new Set(selectedIssueIds);

        // Update in the list
        const updatedBusinessValues = this.businessValues().map((bv) =>
          bv.id === selectedBV.id ? updatedBV : bv
        );
        this.businessValues.set(updatedBusinessValues);
        this.selectedBusinessValue.set(updatedBV);

        // Update connected status
        this.updateIssueSelection(updatedBV);

        this.isSaving.set(false);
      },
      error: (error) => {
        console.error('Failed to save business value connections:', error);
        this.isSaving.set(false);
      },
    });
  }

  private fetchData(releaseId: string): void {
    this.isLoading.set(true);

    forkJoin({
      businessValues: this.businessValueService
        .getBusinessValuesByReleaseId(releaseId)
        .pipe(catchError(() => of([]))),
      issues: this.issueService
        .getIssuesByReleaseId(releaseId)
        .pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ businessValues, issues }) => {
        this.businessValues.set(businessValues);
        this.allIssues.set(issues ?? []);

        // Initialize issues with selection
        const issuesWithSelection: IssueWithSelection[] = (issues ?? []).map((issue) => ({
          ...issue,
          isSelected: false,
          isConnected: false,
        }));
        this.issuesWithSelection.set(issuesWithSelection);

        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  private updateIssueSelection(businessValue: BusinessValue): void {
    const connectedIssueIds = new Set(businessValue.issues?.map((issue) => issue.id) ?? []);

    const updatedIssues = this.issuesWithSelection().map((issue) => ({
      ...issue,
      isSelected: connectedIssueIds.has(issue.id),
      isConnected: connectedIssueIds.has(issue.id),
    }));

    this.issuesWithSelection.set(updatedIssues);
    this.originalSelectedIssueIds = new Set(connectedIssueIds);
  }
}
