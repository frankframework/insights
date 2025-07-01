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
import { GitHubStates } from '../../app.service';

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
  private scheduledMilestones = new Map<string, Date>(); // Cache for scheduled due dates

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

  public clearScheduledDates(): void {
    this.scheduledMilestones.clear();
    this.loadRoadmapData();
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
        map(({ milestones }) => {
          // Always apply scheduling to maintain consistency with cached dates
          return this.scheduleMilestones(milestones);
        }),
        map((finalMilestones) => this.sortMilestones(finalMilestones)),
        map((sortedMilestones) => this.filterMilestonesInView(sortedMilestones)),
        tap((finalMilestones) => {
          this.openMilestones = finalMilestones;
        }),
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
    // Parse versions
    const parsed = milestones
      .map((m) => ({ milestone: m, version: this.parseVersion(m.title) }))
      .filter((mv) => mv.version);

    // Majors en minors splitsen
    const majors = parsed.filter((mv) => mv.version!.patch === 0);
    const minors = parsed.filter((mv) => mv.version!.patch > 0);

    // Anchor bepalen (eerste major met dueOn, anders huidige kwartaal)
    let anchorQuarter: Date;
    let anchorDueOn: Date | null = null;
    const anchorMajor = majors.find((mv) => mv.milestone.dueOn);
    if (anchorMajor && anchorMajor.milestone.dueOn) {
      anchorDueOn = new Date(anchorMajor.milestone.dueOn);
      anchorQuarter = this.getQuarterFromDate(anchorDueOn);
    } else {
      anchorQuarter = this.getQuarterFromDate(new Date());
    }

    // Majors plannen
    let quarterCursor = new Date(anchorQuarter);
    for (const { milestone, version } of majors) {
      if (milestone.dueOn) {
        // Zet quarterCursor op het kwartaal na deze dueOn
        const dueQuarter = this.getQuarterFromDate(new Date(milestone.dueOn));
        quarterCursor = new Date(dueQuarter);
        quarterCursor.setMonth(quarterCursor.getMonth() + 3);
      } else {
        milestone.dueOn = this.getQuarterEndDate(quarterCursor);
        milestone.isEstimated = true;
        quarterCursor.setMonth(quarterCursor.getMonth() + 3);
      }
    }

    // Minors per serie plannen
    const minorsByMajor = new Map<string, { milestone: Milestone; version: Version }[]>();
    for (const mv of minors) {
      const key = `${mv.version!.major}.${mv.version!.minor}`;
      if (!minorsByMajor.has(key)) minorsByMajor.set(key, []);
      minorsByMajor.get(key)!.push(mv as { milestone: Milestone; version: Version });
    }
    for (const [key, minorList] of minorsByMajor) {
      // Plan eerste minor op anchorQuarter, elke volgende een kwartaal verder
      let minorQuarter = new Date(anchorQuarter);
      minorList.sort((a, b) => a.version.patch - b.version.patch);
      for (const mv of minorList) {
        if (!mv.milestone.dueOn) {
          mv.milestone.dueOn = this.getQuarterEndDate(minorQuarter);
          mv.milestone.isEstimated = true;
          minorQuarter.setMonth(minorQuarter.getMonth() + 3);
        }
      }
    }

    // Uniek houden: geen dubbele milestones per versie
    const uniqueMilestones = new Map<string, Milestone>();
    for (const { milestone, version } of parsed) {
      if (version) {
        uniqueMilestones.set(`${version.major}.${version.minor}.${version.patch}`, milestone);
      }
    }

    return [...uniqueMilestones.values()];
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
      // 1. Toon milestone als dueOn in de timeline range valt
      if (milestone.dueOn) {
        const dueDate = new Date(milestone.dueOn);
        if (dueDate.getTime() >= viewStartTime && dueDate.getTime() <= viewEndTime) {
          return true;
        }
      }
      // 2. Toon milestone als er open issues zijn (state === 'open' of GitHubState.Open)
      const issues = this.getIssuesForMilestone(milestone.id);
      const hasOpen = issues.some((issue) => {
        return issue.state === GitHubStates.OPEN;
      });
      return hasOpen;
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
    return milestones.sort((a, b) => {
      // First sort by due date
      const dueComparison = (a.dueOn?.getTime() ?? 0) - (b.dueOn?.getTime() ?? 0);
      if (dueComparison !== 0) return dueComparison;

      // If due dates are the same, sort by title
      return a.title.localeCompare(b.title);
    });
  }

  private getQuarterFromDate(date: Date): Date {
    const year = date.getFullYear();
    const quarterIndex = Math.floor(date.getMonth() / 3);
    return new Date(year, quarterIndex * 3, 1);
  }

  private getQuarterEndDate(quarterStart: Date): Date {
    const quarterEnd = new Date(quarterStart);
    quarterEnd.setMonth(quarterEnd.getMonth() + 3);
    quarterEnd.setDate(0); // Last day of the month
    quarterEnd.setHours(23, 59, 59, 999);
    return quarterEnd;
  }
}
