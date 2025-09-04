import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { OffCanvasComponent } from '../../../components/off-canvas/off-canvas.component';
import { Release } from '../../../services/release.service';
import { Label, LabelService } from '../../../services/label.service';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { ReleaseHighlightsComponent } from './release-highlights/release-highlights.component';
import { Issue, IssueService } from '../../../services/issue.service';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { ReleaseImportantIssuesComponent } from './release-important-issues/release-important-issues.component';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-release-off-canvas',
  standalone: true,
  imports: [LoaderComponent, OffCanvasComponent, ReleaseHighlightsComponent, ReleaseImportantIssuesComponent],
  templateUrl: './release-off-canvas.component.html',
  styleUrl: './release-off-canvas.component.scss',
})
export class ReleaseOffCanvasComponent implements OnChanges {
  @Input() release!: Release;
  @Output() closeCanvas = new EventEmitter<void>();

  public highlightedLabels?: Label[];
  public releaseIssues?: Issue[];
  public isLoading = true;

  private labelService = inject(LabelService);
  private issueService = inject(IssueService);
  private toastService = inject(ToastrService);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['release'] && this.release?.id) {
      this.isLoading = true;
      this.fetchData(this.release.id);
    }
  }

  private fetchData(releaseId: string): void {
    const labels$ = this.labelService.getHighLightsByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load highlights:', error);
        this.toastService.error('Failed to load release highlights. Please try again later.');
        return of();
      }),
    );

    const issues$ = this.issueService.getIssuesByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load issues:', error);
        this.toastService.error('Failed to load release issues. Please try again later.');
        return of();
      }),
    );

    forkJoin({
      labels: labels$,
      issues: issues$,
    })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe(({ labels, issues }) => {
        if (labels && labels.length > 0) {
          this.highlightedLabels = labels;
        } else {
          this.highlightedLabels = undefined;
          if (Array.isArray(labels)) {
            this.toastService.error('No release highlights found.');
          }
        }

        if (issues && issues.length > 0) {
          this.releaseIssues = issues;
        } else {
          this.releaseIssues = undefined;
          if (Array.isArray(issues)) {
            this.toastService.error('No release issues found.');
          }
        }
      });
  }
}
