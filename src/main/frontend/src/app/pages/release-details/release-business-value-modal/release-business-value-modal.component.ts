import { Component, EventEmitter, Input, Output, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { BusinessValue, BusinessValueService } from '../../../services/business-value.service';
import { Issue } from '../../../services/issue.service';
import { IssueTreeBranchComponent } from '../release-important-issues/issue-tree-branch/issue-tree-branch.component';

@Component({
  selector: 'app-release-business-value-modal',
  standalone: true,
  imports: [CommonModule, ModalComponent, IssueTreeBranchComponent],
  templateUrl: './release-business-value-modal.component.html',
  styleUrl: './release-business-value-modal.component.scss',
})
export class ReleaseBusinessValueModalComponent implements OnInit {
  @Input() businessValueId!: string;
  @Output() closed = new EventEmitter<void>();

  public businessValue = signal<BusinessValue | null>(null);
  public isLoading = signal<boolean>(false);
  public errorMessage = signal<string>('');

  private businessValueService = inject(BusinessValueService);

  ngOnInit(): void {
    this.loadBusinessValue();
  }

  public close(): void {
    this.closed.emit();
  }

  public openIssue(issue: Issue): void {
    if (issue.url) {
      window.open(issue.url, '_blank');
    }
  }

  private loadBusinessValue(): void {
    if (!this.businessValueId) return;

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.businessValueService.getBusinessValueById(this.businessValueId).subscribe({
      next: (businessValue) => {
        this.businessValue.set(businessValue);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.errorMessage.set(error.error?.message || 'Failed to load business value details');
        console.error('Failed to load business value:', error);
      },
    });
  }
}
