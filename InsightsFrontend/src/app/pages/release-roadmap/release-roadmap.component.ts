import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, finalize, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { RoadmapToolbarComponent } from './roadmap-toolbar/roadmap-toolbar.component';
import { TimelineHeaderComponent } from './timeline-header/timeline-header.component';
import { MilestoneRowComponent } from './milestone-row/milestone-row.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { Issue, IssueService } from '../../services/issue.service';
import { Milestone, MilestoneService } from '../../services/milestone.service';

interface Version {
  major: number;
  minor: number;
  patch: number;
  source: string;
}

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
  public timelineEndDate!: Date;
  public months: Date[] = [];
  public quarters: { name: string; monthCount: number }[] = [];
  public totalDays = 0;
  public displayDate!: Date;
  public currentPeriodLabel = '';
  protected todayOffsetPercentage = 0;
  private viewInitialized = false;

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
    // Use a small timeout to ensure the DOM has been fully painted after data load
    setTimeout(() => this.scrollToToday(), 0);
  }

  public changePeriod(months: number): void {
    this.displayDate.setMonth(this.displayDate.getMonth() + months);
    this.displayDate = new Date(this.displayDate); // Force change detection
    this.generateTimelineFromPeriod();
  }

  public resetPeriod(): void {
    const today = new Date();
    const currentQuarterStartMonth = Math.floor(today.getMonth() / 3) * 3;
    this.displayDate = new Date(today.getFullYear(), currentQuarterStartMonth, 1);
    this.generateTimelineFromPeriod();
  }

  public getIssuesForMilestone(milestoneId: string): Issue[] {
    return this.milestoneIssues.get(milestoneId) || [];
  }

  private generateTimelineFromPeriod(): void {
    if (!this.displayDate) return;

    this.calculateTimelineBoundaries();
    this.generateMonths();
    this.generateQuarters();
    this.calculateTodayMarkerPosition();
    this.loadRoadmapData();
  }

  private calculateTimelineBoundaries(): void {
    this.timelineStartDate = new Date(this.displayDate);
    const endDate = new Date(this.timelineStartDate);
    endDate.setMonth(endDate.getMonth() + 6);
    endDate.setDate(0);
    this.timelineEndDate = endDate;
  }

  private generateMonths(): void {
    this.months = [];
    let currentDate = new Date(this.timelineStartDate);
    while (currentDate <= this.timelineEndDate) {
      this.months.push(new Date(currentDate));
      currentDate.setMonth(currentDate.getMonth() + 1);
    }
  }

  private calculateTodayMarkerPosition(): void {
    this.totalDays = (this.timelineEndDate.getTime() - this.timelineStartDate.getTime()) / (1000 * 3600 * 24) + 1;
    const today = new Date();
    if (today < this.timelineStartDate || today > this.timelineEndDate) {
      this.todayOffsetPercentage = 0;
      return;
    }

    const totalDuration = this.timelineEndDate.getTime() - this.timelineStartDate.getTime();
    if (totalDuration <= 0) {
      this.todayOffsetPercentage = 0;
      return;
    }
    const todayOffset = today.getTime() - this.timelineStartDate.getTime();
    this.todayOffsetPercentage = (todayOffset / totalDuration) * 100;
  }

  private generateQuarters(): void {
    this.quarters = [];
    if (this.months.length === 0) return;

    const startYear = this.timelineStartDate.getFullYear();
    const startQuarterNumber = Math.floor(this.timelineStartDate.getMonth() / 3) + 1;
    const firstQuarterName = `Q${startQuarterNumber} ${startYear}`;
    this.quarters.push({ name: firstQuarterName, monthCount: 3 });

    const nextQuarterStartDate = new Date(this.timelineStartDate);
    nextQuarterStartDate.setMonth(nextQuarterStartDate.getMonth() + 3);
    const endYear = nextQuarterStartDate.getFullYear();
    const endQuarterNumber = Math.floor(nextQuarterStartDate.getMonth() / 3) + 1;
    const secondQuarterName = `Q${endQuarterNumber} ${endYear}`;
    this.quarters.push({ name: secondQuarterName, monthCount: 3 });

    this.currentPeriodLabel = `${firstQuarterName} - ${secondQuarterName}`;
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
            return of({ milestones: [] });
          }
          const issueRequests = parsedMilestones.map((m) =>
            this.issueService.getIssuesByMilestoneId(m.id).pipe(
              map((issues) => ({ milestoneId: m.id, issues })),
              catchError(() => of({ milestoneId: m.id, issues: [] })),
            ),
          );
          return forkJoin(issueRequests).pipe(
            map((issueResults) => {
              this.milestoneIssues.clear();
              for (const result of issueResults) this.milestoneIssues.set(result.milestoneId, result.issues);
              return { milestones: parsedMilestones };
            }),
          );
        }),
        map(({ milestones }) => this.scheduleMilestones(milestones)),
        map((finalMilestones) => this.sortMilestones(finalMilestones)),
        map((sortedMilestones) => this.filterMilestonesInView(sortedMilestones)),
        tap((finalMilestones) => (this.openMilestones = finalMilestones)),
        catchError((error) => {
          this.toastrService.error('Could not load roadmap data.', 'Error');
          console.error(error);
          return of(null);
        }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
          // Defer scroll until after view has been updated
          setTimeout(() => this.scrollToToday(), 0);
        }),
      )
      .subscribe();
  }

  private scheduleMilestones(milestones: Milestone[]): Milestone[] {
    const sortedByVersion = this.getMilestonesSortedByVersion(milestones);
    const majors = sortedByVersion.filter((m) => m.version!.patch === 0);
    const minors = sortedByVersion.filter((m) => m.version!.patch !== 0);

    const scheduledMajors = this.planReleases(majors, new Date());
    const scheduledMinors = this.planReleases(minors, new Date(), true);

    return [...scheduledMajors, ...scheduledMinors];
  }

  private getMilestonesSortedByVersion(milestones: Milestone[]): { milestone: Milestone; version: Version | null }[] {
    return milestones
      .map((m) => ({ milestone: m, version: this.parseVersion(m.title) }))
      .filter((item) => item.version !== null)
      .sort((a, b) => {
        const vA = a.version!;
        const vB = b.version!;
        if (vA.major !== vB.major) return vA.major - vB.major;
        if (vA.minor !== vB.minor) return vA.minor - vB.minor;
        return vA.patch - vB.patch;
      });
  }

  private planReleases(
    versionedItems: { milestone: Milestone; version: Version | null }[],
    startDate: Date,
    resetCursorForSeries = false,
  ): Milestone[] {
    const scheduled: Milestone[] = [];
    let quarterCursor = new Date(startDate);
    let lastSeries = '';

    for (const item of versionedItems) {
      if (resetCursorForSeries) {
        const series = `${item.version!.major}.${item.version!.minor}`;
        if (series !== lastSeries) {
          quarterCursor = new Date(startDate);
        }
        lastSeries = series;
      }

      const year = quarterCursor.getFullYear();
      const quarterIndex = Math.floor(quarterCursor.getMonth() / 3);
      item.milestone.dueOn = new Date(year, quarterIndex * 3 + 3, 0); // End of quarter
      scheduled.push(item.milestone);
      quarterCursor.setMonth(quarterCursor.getMonth() + 3);
    }
    return scheduled;
  }

  private parseVersion(title: string): Version | null {
    const match = title.match(/(\d+)\.(\d+)\.(\d+)/);
    if (!match) return null;
    return {
      major: Number.parseInt(match[1], 10),
      minor: Number.parseInt(match[2], 10),
      patch: Number.parseInt(match[3], 10),
      source: title,
    };
  }

  private filterMilestonesInView(milestones: Milestone[]): Milestone[] {
    if (!this.timelineStartDate || !this.timelineEndDate) return milestones;
    const viewStartTime = this.timelineStartDate.getTime();
    const viewEndTime = this.timelineEndDate.getTime();

    return milestones.filter((milestone) => {
      if (!milestone.dueOn) return false;
      const dueTime = milestone.dueOn.getTime();
      return dueTime >= viewStartTime && dueTime <= viewEndTime;
    });
  }

  private scrollToToday(): void {
    if (this.viewInitialized && this.scrollContainer?.nativeElement && this.todayOffsetPercentage > 0) {
      const container = this.scrollContainer.nativeElement;
      const titleWidth = window.innerWidth >= 1024 ? 300 : 150;

      const scrollableWidth = container.scrollWidth - container.clientWidth;
      const timelineAreaWidth = container.scrollWidth - titleWidth;
      const todayMarkerAbsolutePosition = (this.todayOffsetPercentage / 100) * timelineAreaWidth;
      const desiredScrollLeft = todayMarkerAbsolutePosition - container.clientWidth / 3;

      container.scrollLeft = Math.max(0, Math.min(desiredScrollLeft, scrollableWidth));
    }
  }

  private parseMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.map((m) => ({ ...m, dueOn: m.dueOn ? new Date(m.dueOn) : null }));
  }

  private sortMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.sort((a, b) => (a.dueOn?.getTime() ?? 0) - (b.dueOn?.getTime() ?? 0));
  }
}
