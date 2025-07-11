import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { OffCanvasComponent } from '../../../components/off-canvas/off-canvas.component';
import { Release } from '../../../services/release.service';
import { Label, LabelService } from '../../../services/label.service';
import { catchError, of } from 'rxjs';
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
      this.getHighlightedLabelsByReleaseId(this.release.id);
      this.getIssuesByReleaseId(this.release.id);
    }
  }

  public colorNameToRgba(color: string): string {
    const temporaryElement = document.createElement('div');
    temporaryElement.style.color = color;
    document.body.append(temporaryElement);

    const rgb = getComputedStyle(temporaryElement).color;
    temporaryElement.remove();

    const match = rgb.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
    if (match) {
      const [, r, g, b] = match;
      return `rgba(${r},${g},${b},${0.75})`;
    }

    return color;
  }

  private getHighlightedLabelsByReleaseId(releaseId: string): void {
    this.labelService
      .getHighLightsByReleaseId(releaseId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load highlights:', error);
          this.highlightedLabels = undefined;
          this.toastService.error('Failed to load release highlights. Please try again later.');
          this.isLoading = false;
          return of();
        }),
      )
      .subscribe((highlightedLabels: Label[] | undefined) => {
        if (!highlightedLabels || highlightedLabels.length === 0) {
          this.toastService.error('No release highlights found.');
          this.highlightedLabels = undefined;
        } else {
          this.highlightedLabels = highlightedLabels;
        }
        this.isLoading = false;
      });
  }

  private getIssuesByReleaseId(releaseId: string): void {
    this.issueService
      .getIssuesByReleaseId(releaseId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load issues:', error);
          this.releaseIssues = undefined;
          this.toastService.error('Failed to load release issues. Please try again later.');
          this.isLoading = false;
          return of();
        }),
      )
      .subscribe((releaseIssues: Issue[] | undefined) => {
        if (!releaseIssues || releaseIssues.length === 0) {
          this.toastService.error('No release issues found.');
          this.releaseIssues = undefined;
        } else {
          this.releaseIssues = releaseIssues;
        }
        this.isLoading = false;
      });
  }
}
