import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { OffCanvasComponent } from '../../../components/off-canvas/off-canvas.component';
import { Release } from '../../../services/release.service';
import { Label, LabelService } from '../../../services/label.service';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { catchError, of } from 'rxjs';
import { ReleaseHighlightsComponent } from './release-highlights/release-highlights.component';
import { Issue, IssueService } from '../../../services/issue.service';

@Component({
  selector: 'app-release-off-canvas',
  standalone: true,
  imports: [OffCanvasComponent, NgxChartsModule, ReleaseHighlightsComponent, ReleaseHighlightsComponent],
  templateUrl: './release-off-canvas.component.html',
  styleUrl: './release-off-canvas.component.scss',
})
export class ReleaseOffCanvasComponent implements OnChanges {
  @Input() release!: Release;
  @Output() closeCanvas = new EventEmitter<void>();

  public highlightedLabels?: Label[];
  public releaseIssues?: Issue[];

  constructor(
    private labelService: LabelService,
    private issueService: IssueService,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['release'] && this.release?.id) {
      this.getHighlightedLabelsByReleaseId(this.release.id);
      this.getIssuesByReleaseId(this.release.id);
    }
  }

  private getHighlightedLabelsByReleaseId(releaseId: string): void {
    this.labelService
      .getHighLightsByReleaseId(releaseId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load highlights:', error);
          this.highlightedLabels = undefined;
          return of();
        }),
      )
      .subscribe((highlightedLabels: Label[] | undefined) => {
        if (!highlightedLabels) {
          this.highlightedLabels = undefined;
          return;
        }
        this.highlightedLabels = highlightedLabels;
      });
  }

  private getIssuesByReleaseId(releaseId: string): void {
    this.issueService
      .getIssuesByReleaseId(releaseId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load release issues:', error);
          this.releaseIssues = undefined;
          return of();
        }),
      )
      .subscribe((releaseIssues: Issue[] | undefined) => {
        if (!releaseIssues) {
          this.releaseIssues = undefined;
          return;
        }
        this.releaseIssues = releaseIssues;
      });
  }
}
