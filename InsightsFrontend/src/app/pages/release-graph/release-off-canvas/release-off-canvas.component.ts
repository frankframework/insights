import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { OffCanvasComponent } from '../../../components/off-canvas/off-canvas.component';
import { Release } from '../../../services/release.service';
import { Label, LabelService } from '../../../services/label.service';
import { catchError, of } from 'rxjs';
import { ReleaseHighlightsComponent } from './release-highlights/release-highlights.component';
import { Issue, IssueService } from '../../../services/issue.service';
import { LoaderComponent } from '../../../components/loader/loader.component';
import { ReleaseImportantIssuesComponent } from './release-important-issues/release-important-issues.component';

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

  constructor(
    private labelService: LabelService,
    private issueService: IssueService,
  ) {}

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
          this.checkOffCanvasLoading();
          return of();
        }),
      )
      .subscribe((highlightedLabels: Label[] | undefined) => {
        if (!highlightedLabels) {
          this.highlightedLabels = undefined;
          return;
        }
        this.highlightedLabels = highlightedLabels;
        this.checkOffCanvasLoading();
      });
  }

  private getIssuesByReleaseId(releaseId: string): void {
    this.issueService
      .getIssuesByReleaseId(releaseId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load release issues:', error);
          this.releaseIssues = undefined;
          this.checkOffCanvasLoading();
          return of();
        }),
      )
      .subscribe((releaseIssues: Issue[] | undefined) => {
        if (!releaseIssues) {
          this.releaseIssues = undefined;
          return;
        }
        this.releaseIssues = releaseIssues;
        this.checkOffCanvasLoading();
      });
  }

  private checkOffCanvasLoading(): void {
    if (this.highlightedLabels !== undefined && this.releaseIssues !== undefined) {
      this.isLoading = false;
    }
  }
}
