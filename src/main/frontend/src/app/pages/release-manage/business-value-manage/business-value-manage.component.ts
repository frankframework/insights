import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { BusinessValue, BusinessValueService } from '../../../services/business-value.service';
import { Issue, IssueService } from '../../../services/issue.service';
import { ReleaseService } from '../../../services/release.service';
import { catchError, of, forkJoin } from 'rxjs';
import { BusinessValueAddComponent } from './business-value-add/business-value-add.component';
import { BusinessValueEditComponent } from './business-value-edit/business-value-edit.component';
import { BusinessValueDeleteComponent } from './business-value-delete/business-value-delete.component';
import { LoaderComponent } from '../../../components/loader/loader.component';

interface IssueWithSelection extends Issue {
  isSelected: boolean;
  isConnected: boolean;
  assignedToOther?: boolean;
  assignedBusinessValueTitle?: string;
}

@Component({
  selector: 'app-business-value-manage',
  standalone: true,
  imports: [
    CommonModule,
    BusinessValueAddComponent,
    BusinessValueEditComponent,
    BusinessValueDeleteComponent,
    FormsModule,
    LoaderComponent,
  ],
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
  public releaseTitle = signal<string>('');

  public showCreateForm = signal<boolean>(false);
  public showEditForm = signal<boolean>(false);
  public showDeleteForm = signal<boolean>(false);

  public businessValueToDelete = signal<BusinessValue | null>(null);

  public issueSearchQuery = signal<string>('');
  public businessValueSearchQuery = signal<string>('');

  // eslint-disable-next-line unicorn/consistent-function-scoping
  public filteredBusinessValues = computed(() => {
    const query = this.businessValueSearchQuery().toLowerCase().trim();
    let values = this.businessValues();

    if (query) {
      values = values.filter((bv) => this.filterBusinessValuesByQuery(bv, query));
    }

    return [...values].toSorted(this.sortBusinessValuesByIssueCount);
  });

  // eslint-disable-next-line unicorn/consistent-function-scoping
  public hasChanges = computed(() => this.hasIssueChanges());

  // eslint-disable-next-line unicorn/consistent-function-scoping
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

  private route = inject(ActivatedRoute);
  private location = inject(Location);
  private businessValueService = inject(BusinessValueService);
  private issueService = inject(IssueService);
  private releaseService = inject(ReleaseService);

  private originalSelectedIssueIds = signal<Set<string>>(new Set());

  ngOnInit(): void {
    const releaseId = this.route.snapshot.paramMap.get('id');
    if (releaseId) {
      this.releaseId.set(releaseId);
      this.fetchData(releaseId);
    }
  }

  public goBack(): void {
    this.location.back();
  }

  public toggleCreateForm(): void {
    this.showCreateForm.update((value) => !value);
  }

  public toggleEditForm(): void {
    this.showEditForm.update((value) => !value);
  }

  public updateIssueSearchQuery(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.issueSearchQuery.set(query);
  }

  public updateBusinessValueSearchQuery(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    this.businessValueSearchQuery.set(query);
  }

  public openDeleteModal(businessValue: BusinessValue, event: Event): void {
    event.stopPropagation();
    this.businessValueToDelete.set(businessValue);
    this.showDeleteForm.set(true);
  }

  public closeDeleteModal(): void {
    this.showDeleteForm.set(false);
    this.businessValueToDelete.set(null);
  }

  public onBusinessValueDeleted(deletedId: string): void {
    this.businessValues.update((list) => list.filter((item) => item.id !== deletedId));

    if (this.selectedBusinessValue()?.id === deletedId) {
      this.selectedBusinessValue.set(null);
      this.resetIssueSelection();
    } else {
      if (this.selectedBusinessValue()) {
        this.updateIssueSelection(this.selectedBusinessValue()!);
      }
    }

    this.closeDeleteModal();
  }

  public onBusinessValueUpdated(updatedBusinessValue: BusinessValue): void {
    const updatedList = this.businessValues().map((bv) =>
      bv.id === updatedBusinessValue.id ? updatedBusinessValue : bv,
    );
    this.businessValues.set(updatedList);
    this.selectedBusinessValue.set(updatedBusinessValue);
  }

  public onBusinessValueCreated(businessValue: BusinessValue): void {
    const updatedList = [...this.businessValues(), businessValue];
    this.businessValues.set(updatedList);
  }

  public selectBusinessValue(businessValue: BusinessValue): void {
    if (this.selectedBusinessValue()?.id === businessValue.id) {
      this.selectedBusinessValue.set(null);
      this.resetIssueSelection();
    } else {
      this.selectedBusinessValue.set(businessValue);
      this.businessValueService.getBusinessValueById(businessValue.id).subscribe({
        next: (detailedBV) => {
          const updatedList = this.businessValues().map((bv) => (bv.id === detailedBV.id ? detailedBV : bv));
          this.businessValues.set(updatedList);
          this.selectedBusinessValue.set(detailedBV);
          this.updateIssueSelection(detailedBV);
        },
        error: (error) => {
          console.error('Failed to fetch business value details', error);
        },
      });
    }
  }

  public toggleIssue(issue: IssueWithSelection): void {
    this.handleToggleIssue(issue);
  }

  public saveChanges(): void {
    const selectedBV = this.selectedBusinessValue();
    if (!selectedBV) return;
    this.performSaveChanges(selectedBV);
  }

  private sortIssuesByPriority = (
    a: IssueWithSelection,
    b: IssueWithSelection,
    selectedBV: BusinessValue | null,
  ): number => {
    if (!selectedBV) return a.number - b.number;

    if (a.isConnected && !b.isConnected) return -1;
    if (!a.isConnected && b.isConnected) return 1;

    const aFree = !a.isConnected && !a.assignedToOther;
    const bFree = !b.isConnected && !b.assignedToOther;
    if (aFree && !bFree) return -1;
    if (!aFree && bFree) return 1;

    return a.number - b.number;
  };

  private hasIssueChanges = (): boolean => {
    const originalIds = this.originalSelectedIssueIds();
    const currentSelectedIds = new Set(
      this.issuesWithSelection()
        .filter((issue) => issue.isSelected)
        .map((issue) => issue.id),
    );

    if (currentSelectedIds.size !== originalIds.size) return true;
    for (const id of currentSelectedIds) {
      if (!originalIds.has(id)) return true;
    }
    return false;
  };

  private filterBusinessValuesByQuery = (bv: BusinessValue, query: string): boolean => {
    return bv.title.toLowerCase().includes(query);
  };

  private sortBusinessValuesByIssueCount = (a: BusinessValue, b: BusinessValue): number => {
    const countA = a.issues?.length || 0;
    const countB = b.issues?.length || 0;
    return countB - countA;
  };

  private resetIssueSelection(): void {
    const updatedIssues = this.issuesWithSelection().map((issue) => ({
      ...issue,
      isSelected: false,
      isConnected: false,
      assignedToOther: false,
      assignedBusinessValueTitle: undefined,
    }));
    this.issuesWithSelection.set(updatedIssues);
    this.originalSelectedIssueIds.set(new Set());
  }

  private handleToggleIssue(issue: IssueWithSelection): void {
    if (issue.assignedToOther) return;

    const issues = this.issuesWithSelection();
    const updatedIssues = issues.map((index) =>
      index.id === issue.id ? { ...index, isSelected: !index.isSelected } : index,
    );
    this.issuesWithSelection.set(updatedIssues);
  }

  private performSaveChanges(selectedBV: BusinessValue): void {
    this.isSaving.set(true);

    const selectedIssueIds = this.issuesWithSelection()
      .filter((issue) => issue.isSelected)
      .map((issue) => issue.id);

    this.businessValueService.updateIssueConnections(selectedBV.id, selectedIssueIds).subscribe({
      next: (updatedBV) => {
        const updatedBusinessValues = this.businessValues().map((bv) => (bv.id === selectedBV.id ? updatedBV : bv));
        this.businessValues.set(updatedBusinessValues);
        this.selectedBusinessValue.set(updatedBV);
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
      businessValues: this.businessValueService.getAllBusinessValues().pipe(catchError(() => of([]))),
      issues: this.issueService.getIssuesByReleaseId(releaseId).pipe(catchError(() => of([]))),
      release: this.releaseService.getReleaseById(releaseId).pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ businessValues, issues, release }) => {
        this.businessValues.set(businessValues);
        this.allIssues.set(issues ?? []);

        if (release) {
          const releaseData = release as { name?: string; title?: string };
          this.releaseTitle.set(releaseData.name || releaseData.title || releaseId);
        } else {
          this.releaseTitle.set(releaseId);
        }

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

  private updateIssueSelection(currentBusinessValue: BusinessValue): void {
    const currentConnectedIds = new Set(currentBusinessValue.issues?.map((issue) => issue.id));

    const assignedToOthersMap = new Map<string, string>();
    for (const bv of this.businessValues()) {
      if (bv.id === currentBusinessValue.id) continue;
      if (bv.issues)
        for (const issue of bv.issues) {
          assignedToOthersMap.set(issue.id, bv.title);
        }
    }

    const updatedIssues = this.allIssues().map((issue) => {
      const isConnectedToCurrent = currentConnectedIds.has(issue.id);
      const otherBvTitle = assignedToOthersMap.get(issue.id);
      const isAssignedToOther = !!otherBvTitle;

      return {
        ...issue,
        isSelected: isConnectedToCurrent,
        isConnected: isConnectedToCurrent,
        assignedToOther: isAssignedToOther,
        assignedBusinessValueTitle: otherBvTitle,
      } as IssueWithSelection;
    });

    this.issuesWithSelection.set(updatedIssues);
    this.originalSelectedIssueIds.set(currentConnectedIds);
  }
}
