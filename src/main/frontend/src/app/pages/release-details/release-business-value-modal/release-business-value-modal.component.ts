import { Component, EventEmitter, Input, Output, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { BusinessValue } from '../../../services/business-value.service';
import { Issue } from '../../../services/issue.service';
import { IssueTreeBranchComponent } from '../release-important-issues/issue-tree-branch/issue-tree-branch.component';

@Component({
  selector: 'app-release-business-value-modal',
  standalone: true,
  imports: [CommonModule, ModalComponent, IssueTreeBranchComponent],
  templateUrl: './release-business-value-modal.component.html',
  styleUrl: './release-business-value-modal.component.scss',
})
export class ReleaseBusinessValueModalComponent {
  @Output() closed = new EventEmitter<void>();

  public businessValueSignal: WritableSignal<BusinessValue | null> = signal(null);

  public get businessValue(): WritableSignal<BusinessValue | null> {
    return this.businessValueSignal;
  }

  @Input({ required: true }) set businessValue(value: BusinessValue | null) {
    this.businessValueSignal.set(value);
  }

  public close(): void {
    this.closed.emit();
  }

  public openIssue(issue: Issue): void {
    if (issue.url) {
      window.open(issue.url, '_blank');
    }
  }
}
