import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, forkJoin, of, switchMap, tap, finalize, map } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { RoadmapToolbarComponent } from './roadmap-toolbar/roadmap-toolbar.component';
import { TimelineHeaderComponent } from './timeline-header/timeline-header.component';
import { MilestoneRowComponent } from './milestone-row/milestone-row.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { Issue, IssueService } from '../../services/issue.service';
import { Milestone, MilestoneService } from '../../services/milestone.service';

@Component({
  selector: 'app-release-roadmap',
  standalone: true,
  imports: [CommonModule, RoadmapToolbarComponent, TimelineHeaderComponent, MilestoneRowComponent, LoaderComponent],
  templateUrl: './release-roadmap.component.html',
  styleUrls: ['./release-roadmap.component.scss'],
})
export class ReleaseRoadmapComponent implements OnInit, AfterViewInit {
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLDivElement>;

  public isLoading = true;
  public openMilestones: Milestone[] = [];
  public milestoneIssues = new Map<string, Issue[]>();
  public timelineStartDate!: Date;
  public months: Date[] = [];
  public quarters: { name: string; monthCount: number }[] = [];
  public totalDays = 0;

  public displayDate!: Date;
  protected todayOffsetPercentage = 0;
  private viewInitialized = false;
  private readonly DEFAULT_POINTS_ESTIMATE = 3;

  constructor(
    private milestoneService: MilestoneService,
    private issueService: IssueService,
    private toastrService: ToastrService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.resetPeriod();
  }

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.scrollToToday();
  }

  public changePeriod(months: number): void {
    this.displayDate.setMonth(this.displayDate.getMonth() + months);
    this.displayDate = new Date(this.displayDate);
    this.generateTimelineFromPeriod();
  }

  public resetPeriod(): void {
    this.displayDate = new Date();
    this.generateTimelineFromPeriod();
  }

  public getIssuesForMilestone(milestoneId: string): Issue[] {
    return this.milestoneIssues.get(milestoneId) || [];
  }

  private generateTimelineFromPeriod(): void {
    if (!this.displayDate) return;

    const startMonth = this.displayDate.getMonth() < 6 ? 0 : 6;
    const year = this.displayDate.getFullYear();
    const startDate = new Date(year, startMonth, 1);
    const endDate = new Date(year, startMonth + 6, 1);

    this.timelineStartDate = startDate;

    this.months = [];
    let currentDate = new Date(this.timelineStartDate);
    while (currentDate < endDate) {
      this.months.push(new Date(currentDate));
      currentDate.setMonth(currentDate.getMonth() + 1);
    }

    this.generateQuarters();

    this.totalDays = (endDate.getTime() - startDate.getTime()) / (1000 * 3600 * 24);

    const today = new Date();
    const daysFromStart = (today.getTime() - this.timelineStartDate.getTime()) / (1000 * 3600 * 24);
    this.todayOffsetPercentage = (daysFromStart / this.totalDays) * 100;

    this.loadRoadmapData();
  }

  private loadRoadmapData(): void {
    this.isLoading = true;
    this.milestoneService
      .getOpenMilestones()
      .pipe(
        map((apiMilestones) => this.parseMilestones(apiMilestones)),
        switchMap((parsedMilestones) => {
          if (parsedMilestones.length === 0) {
            this.openMilestones = [];
            return of({ milestones: [], issues: [] });
          }
          const issueRequests = parsedMilestones.map((m) =>
            this.issueService.getIssuesByMilestoneId(m.id).pipe(
              map((issues) => ({ milestoneId: m.id, issues })),
              catchError(() => of({ milestoneId: m.id, issues: [] })),
            ),
          );
          return forkJoin(issueRequests).pipe(
            map((issueResults) => {
              for (const result of issueResults) this.milestoneIssues.set(result.milestoneId, result.issues);
              return { milestones: parsedMilestones };
            }),
          );
        }),
        map(({ milestones }) => this.estimateDueDates(milestones)),
        map((finalMilestones) => this.sortMilestones(finalMilestones)),
        tap((finalMilestones) => (this.openMilestones = finalMilestones)),
        catchError((error) => {
          this.toastrService.error('Kon roadmap data niet laden.', 'Fout');
          console.error(error);
          return of(null);
        }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
          this.scrollToToday();
        }),
      )
      .subscribe();
  }

  private estimateDueDates(milestones: Milestone[]): Milestone[] {
    const { withDate, withoutDate } = milestones.reduce(
      (accumulator, m) => {
        (m.dueOn ? accumulator.withDate : accumulator.withoutDate).push(m);
        return accumulator;
      },
      { withDate: [] as Milestone[], withoutDate: [] as Milestone[] },
    );

    // eslint-disable-next-line unicorn/prefer-math-min-max
    let lastDueDate = withDate.reduce((latest, m) => (m.dueOn! > latest ? m.dueOn! : latest), new Date(1970, 0, 1));

    if (lastDueDate.getFullYear() === 1970) {
      lastDueDate = new Date();
    }

    const estimatedMilestones = withoutDate.map((milestone) => {
      const issues = this.milestoneIssues.get(milestone.id) || [];
      const totalDuration = issues.reduce(
        (accumulator, issue) => accumulator + (issue.points ?? this.DEFAULT_POINTS_ESTIMATE),
        0,
      );

      lastDueDate.setDate(lastDueDate.getDate() + 1);
      const estimatedDueDate = new Date(lastDueDate);
      estimatedDueDate.setDate(estimatedDueDate.getDate() + totalDuration);

      lastDueDate = estimatedDueDate;

      return { ...milestone, dueOn: estimatedDueDate, isEstimated: true };
    });

    return [...withDate, ...estimatedMilestones];
  }

  private scrollToToday(): void {
    if (this.viewInitialized && this.scrollContainer?.nativeElement && this.todayOffsetPercentage > 0) {
      const container = this.scrollContainer.nativeElement;
      const scrollWidth = container.scrollWidth;
      const scrollTo = (this.todayOffsetPercentage / 100) * scrollWidth;
      container.scrollLeft = scrollTo - container.clientWidth / 3;
    }
  }

  private parseMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.map((m) => ({
      ...m,
      dueOn: m.dueOn ? new Date(m.dueOn) : null,
    }));
  }

  private sortMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.sort((a, b) => {
      if (!a.dueOn) return 1;
      if (!b.dueOn) return -1;
      return a.dueOn.getTime() - b.dueOn.getTime();
    });
  }

  private generateQuarters(): void {
    this.quarters = [];
    if (this.months.length === 0) return;
    let currentQuarter = -1;
    for (const monthDate of this.months) {
      const quarterIndex = Math.floor(monthDate.getMonth() / 3);
      if (quarterIndex === currentQuarter) {
        const lastQuarter = this.quarters.at(-1);
        if (lastQuarter) {
          lastQuarter.monthCount++;
        }
      } else {
        currentQuarter = quarterIndex;
        this.quarters.push({ name: `Q${quarterIndex + 1}`, monthCount: 1 });
      }
    }
  }
}
