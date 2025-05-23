import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { OffCanvasComponent } from '../../../components/off-canvas/off-canvas.component';
import { Release } from '../../../services/release.service';
import { LabelService, ReleaseHighlights } from '../../../services/label.service';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { catchError, of } from 'rxjs';
import { ReleaseHighlightsComponent } from './release-highlights/release-highlights.component';

@Component({
  selector: 'app-release-off-canvas',
  standalone: true,
  imports: [OffCanvasComponent, NgxChartsModule, ReleaseHighlightsComponent],
  templateUrl: './release-off-canvas.component.html',
  styleUrl: './release-off-canvas.component.scss',
})
export class ReleaseOffCanvasComponent implements OnChanges {
  @Input() release!: Release;
  @Output() closeCanvas = new EventEmitter<void>();

  public highlights?: ReleaseHighlights;

  constructor(private labelService: LabelService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['release'] && this.release?.id) {
      this.getHighlightsByReleaseId(this.release.id);
    }
  }

  private getHighlightsByReleaseId(id: string): void {
    this.labelService
      .getHighLightsByReleaseId(id)
      .pipe(
        catchError((error) => {
          console.error('Failed to load highlights:', error);
          this.highlights = undefined;
          return of();
        }),
      )
      .subscribe((highlights: ReleaseHighlights | undefined) => {
        if (!highlights) {
          this.highlights = undefined;
          return;
        }
        this.highlights = highlights;
      });
  }
}
