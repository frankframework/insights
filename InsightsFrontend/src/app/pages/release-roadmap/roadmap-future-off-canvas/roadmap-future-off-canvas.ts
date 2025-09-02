import { Component, OnInit, inject, ChangeDetectorRef, OnDestroy, Output, EventEmitter } from '@angular/core';
import { Subject, catchError, of, takeUntil } from 'rxjs';
import { FutureEpic } from './future-epic/future-epic';
import { Issue, IssueService } from '../../../services/issue.service';
import { OffCanvasComponent } from '../../../components/off-canvas/off-canvas.component';
import { LoaderComponent } from '../../../components/loader/loader.component';

@Component({
  selector: 'app-roadmap-future-off-canvas',
  standalone: true,
  imports: [LoaderComponent, OffCanvasComponent, FutureEpic],
  templateUrl: './roadmap-future-off-canvas.html',
  styleUrl: './roadmap-future-off-canvas.scss',
})
export class RoadmapFutureOffCanvasComponent implements OnInit, OnDestroy {
  @Output() closeCanvas = new EventEmitter<void>();

  public readonly OFF_CANVAS_NAME = 'Future Plans';

  public futureEpicIssues?: Issue[];
  public isLoading = true;

  private readonly destroy$ = new Subject<void>();
  private readonly issueService = inject(IssueService);
  private readonly cdr = inject(ChangeDetectorRef);

  public ngOnInit(): void {
    this.fetchData();
  }

  public ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private fetchData(): void {
    this.isLoading = true;
    this.cdr.markForCheck();

    this.issueService
      .getFutureEpicIssues()
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          console.error('Failed to load future plans:', error);
          return of();
        }),
      )
      .subscribe({
        next: (issues: Issue[]) => {
          this.futureEpicIssues = issues && issues.length > 0 ? issues : undefined;
        },
        complete: () => {
          this.isLoading = false;
          this.cdr.markForCheck();
        },
      });
  }
}
