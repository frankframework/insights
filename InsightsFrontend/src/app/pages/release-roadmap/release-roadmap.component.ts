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
    // Separate milestones with existing due dates from those without
    const milestonesWithDueDate = milestones.filter((m) => m.dueOn);
    const milestonesWithoutDueDate = milestones.filter((m) => !m.dueOn);

    // Apply cached due dates first
    const milestonesWithCachedDates = milestonesWithoutDueDate.map((milestone) => {
      const cachedDate = this.scheduledMilestones.get(milestone.id);
      if (cachedDate) {
        milestone.dueOn = new Date(cachedDate);
        milestone.isEstimated = true;
        return milestone;
      }
      return milestone;
    });

    // Only schedule milestones that don't have a due date and aren't cached
    const milestonesToSchedule = milestonesWithCachedDates.filter((m) => !m.dueOn);
    const sortedByVersion = this.getMilestonesSortedByVersion(milestonesToSchedule);
    const majors = sortedByVersion.filter((m) => m.version!.patch === 0);
    const minors = sortedByVersion.filter((m) => m.version!.patch > 0);

    // Find the lowest major version to use as planning base
    const lowestMajor = this.findLowestMajorVersion(milestones);
    const planningStartDate = this.timelineStartDate || new Date();

    // Plan majors first, then minors within their major series
    const newlyScheduledMilestones = this.planMilestonesByVersion(majors, minors, planningStartDate, lowestMajor);

    // Cache the newly scheduled due dates
    for (const milestone of newlyScheduledMilestones) {
      if (milestone.dueOn) {
        this.scheduledMilestones.set(milestone.id, new Date(milestone.dueOn));
      }
    }

    // Return existing milestones with their original due dates + cached ones + newly scheduled ones
    return [...milestonesWithDueDate, ...milestonesWithCachedDates.filter((m) => m.dueOn), ...newlyScheduledMilestones];
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

  private findLowestMajorVersion(milestones: Milestone[]): number {
    let lowestMajor = Infinity;

    for (const milestone of milestones) {
      const version = this.parseVersion(milestone.title);
      if (version && version.patch === 0) {
        // Only consider majors (X.Y.0)
        lowestMajor = Math.min(lowestMajor, version.major);
      }
    }

    return lowestMajor === Infinity ? 1 : lowestMajor;
  }

  private planMilestonesByVersion(
    majors: { milestone: Milestone; version: Version | null }[],
    minors: { milestone: Milestone; version: Version | null }[],
    startDate: Date,
    lowestMajor: number
  ): Milestone[] {
    const scheduled: Milestone[] = [];
    // Anchor zoeken
    const anchor = majors.find(m => m.milestone.dueOn);
    if (!anchor) {
      return this.planMilestonesByVersionFallback(majors, minors, startDate, lowestMajor);
    }
    const anchorVersion = anchor.version!;
    const anchorQuarter = this.getQuarterFromDate(new Date(anchor.milestone.dueOn!));

    // Majors plannen
    for (const item of majors) {
      if (item.milestone.dueOn) {
        scheduled.push(item.milestone);
        continue;
      }
      const cachedDate = this.scheduledMilestones.get(item.milestone.id);
      if (cachedDate) {
        item.milestone.dueOn = new Date(cachedDate);
        item.milestone.isEstimated = true;
        scheduled.push(item.milestone);
        continue;
      }
      // Verschil in kwartalen tov anchor
      const diff = (item.version!.major - anchorVersion.major) * 10 + (item.version!.minor - anchorVersion.minor);
      const targetQuarter = new Date(anchorQuarter);
      targetQuarter.setMonth(targetQuarter.getMonth() + diff * 3);
      const quarterEnd = this.getQuarterEndDate(targetQuarter);
      item.milestone.dueOn = quarterEnd;
      item.milestone.isEstimated = true;
      scheduled.push(item.milestone);
    }

    // Minors: per patch een kwartaal verder vanaf major (eerste minor = kwartaal NA major)
    const minorsByMajor = new Map<string, { milestone: Milestone; version: Version | null }[]>();
    for (const item of minors) {
      if (!item.version) continue;
      const key = `${item.version.major}.${item.version.minor}`;
      if (!minorsByMajor.has(key)) minorsByMajor.set(key, []);
      minorsByMajor.get(key)!.push(item);
    }
    for (const [key, minorList] of minorsByMajor) {
      // Vind de major milestone voor deze serie
      const [major, minor] = key.split('.').map(Number);
      const majorMilestone = scheduled.find(m => {
        const v = this.parseVersion(m.title);
        return v && v.major === major && v.minor === minor && v.patch === 0;
      });
      if (!majorMilestone) continue;
      const majorDueOn = this.getQuarterFromDate(new Date(majorMilestone.dueOn!));
      // Sorteer minors op patch
      minorList.sort((a, b) => (a.version!.patch - b.version!.patch));
      for (let i = 0; i < minorList.length; i++) {
        const item = minorList[i];
        if (item.milestone.dueOn) {
          scheduled.push(item.milestone);
          continue;
        }
        const cachedDate = this.scheduledMilestones.get(item.milestone.id);
        if (cachedDate) {
          item.milestone.dueOn = new Date(cachedDate);
          item.milestone.isEstimated = true;
          scheduled.push(item.milestone);
          continue;
        }
        // Eerste minor: kwartaal NA major, tweede: +2 kwartalen, etc.
        const targetQuarter = new Date(majorDueOn);
        targetQuarter.setMonth(targetQuarter.getMonth() + (i + 1) * 3);
        const quarterEnd = this.getQuarterEndDate(targetQuarter);
        item.milestone.dueOn = quarterEnd;
        item.milestone.isEstimated = true;
        scheduled.push(item.milestone);
      }
    }
    return scheduled;
  }

  private findReferenceMilestone(
    majors: { milestone: Milestone; version: Version | null }[],
    minors: { milestone: Milestone; version: Version | null }[],
  ): Milestone | null {
    // Look for 9.2.0 milestone with dueOn date
    const allMilestones = [...majors, ...minors];

    for (const item of allMilestones) {
      if (
        item.version &&
        item.version.major === 9 &&
        item.version.minor === 2 &&
        item.version.patch === 0 &&
        item.milestone.dueOn
      ) {
        return item.milestone;
      }
    }

    return null;
  }

  private getQuarterFromDate(date: Date): Date {
    const year = date.getFullYear();
    const quarterIndex = Math.floor(date.getMonth() / 3);
    return new Date(year, quarterIndex * 3, 1);
  }

  private getNextQuarter(quarterStart: Date): Date {
    const nextQuarter = new Date(quarterStart);
    nextQuarter.setMonth(nextQuarter.getMonth() + 3);
    return nextQuarter;
  }

  private getQuarterEndDate(quarterStart: Date): Date {
    const quarterEnd = new Date(quarterStart);
    quarterEnd.setMonth(quarterEnd.getMonth() + 3);
    quarterEnd.setDate(0); // Last day of the month
    quarterEnd.setHours(23, 59, 59, 999);
    return quarterEnd;
  }

  private planMilestonesByVersionFallback(
    majors: { milestone: Milestone; version: Version | null }[],
    minors: { milestone: Milestone; version: Version | null }[],
    startDate: Date,
    lowestMajor: number,
  ): Milestone[] {
    const scheduled: Milestone[] = [];
    let quarterCursor = new Date(startDate);

    // First, plan all majors sequentially
    for (const major of majors) {
      if (major.milestone.dueOn) {
        scheduled.push(major.milestone);
        continue;
      }

      // Check if this milestone has a cached due date
      const cachedDate = this.scheduledMilestones.get(major.milestone.id);
      if (cachedDate) {
        major.milestone.dueOn = new Date(cachedDate);
        major.milestone.isEstimated = true;
        scheduled.push(major.milestone);
        continue;
      }

      // Find the next quarter that falls within our timeline view
      const timelineStart = this.timelineStartDate;
      const timelineEnd = this.timelineEndDate;

      let targetQuarter = new Date(quarterCursor);

      // If the target quarter is before our timeline, move to the first quarter in our view
      if (targetQuarter < timelineStart) {
        targetQuarter = new Date(timelineStart);
      }

      // If the target quarter is after our timeline, skip this milestone
      if (targetQuarter > timelineEnd) {
        continue;
      }

      // Calculate the quarter boundaries
      const year = targetQuarter.getFullYear();
      const quarterIndex = Math.floor(targetQuarter.getMonth() / 3);
      const quarterStart = new Date(year, quarterIndex * 3, 1);
      const quarterEnd = new Date(year, quarterIndex * 3 + 3, 0);
      quarterEnd.setHours(23, 59, 59, 999);

      // Set the milestone due date to the end of the quarter
      major.milestone.dueOn = quarterEnd;
      major.milestone.isEstimated = true;
      scheduled.push(major.milestone);

      // Move to next quarter for next major
      quarterCursor.setMonth(quarterCursor.getMonth() + 3);
    }

    // Then, plan minors within their major series
    // Group minors by their major version
    const minorsByMajor = new Map<number, { milestone: Milestone; version: Version | null }[]>();

    for (const minor of minors) {
      if (minor.version) {
        const majorVersion = minor.version.major;
        if (!minorsByMajor.has(majorVersion)) {
          minorsByMajor.set(majorVersion, []);
        }
        minorsByMajor.get(majorVersion)!.push(minor);
      }
    }

    // For each major series, plan the minors
    for (const [majorVersion, minorList] of minorsByMajor) {
      // Find the corresponding major milestone to get its due date
      const correspondingMajor = scheduled.find((m) => {
        const version = this.parseVersion(m.title);
        return version && version.major === majorVersion && version.patch === 0; // Major is X.Y.0
      });

      if (correspondingMajor && correspondingMajor.dueOn) {
        // Plan minors in the same quarter as their major, or in subsequent quarters
        let minorQuarterCursor = new Date(correspondingMajor.dueOn);

        for (const minor of minorList) {
          if (minor.milestone.dueOn) {
            scheduled.push(minor.milestone);
            continue;
          }

          // Check if this milestone has a cached due date
          const cachedDate = this.scheduledMilestones.get(minor.milestone.id);
          if (cachedDate) {
            minor.milestone.dueOn = new Date(cachedDate);
            minor.milestone.isEstimated = true;
            scheduled.push(minor.milestone);
            continue;
          }

          // Find the next quarter that falls within our timeline view
          const timelineStart = this.timelineStartDate;
          const timelineEnd = this.timelineEndDate;

          let targetQuarter = new Date(minorQuarterCursor);

          // If the target quarter is before our timeline, move to the first quarter in our view
          if (targetQuarter < timelineStart) {
            targetQuarter = new Date(timelineStart);
          }

          // If the target quarter is after our timeline, skip this milestone
          if (targetQuarter > timelineEnd) {
            continue;
          }

          // Calculate the quarter boundaries
          const year = targetQuarter.getFullYear();
          const quarterIndex = Math.floor(targetQuarter.getMonth() / 3);
          const quarterStart = new Date(year, quarterIndex * 3, 1);
          const quarterEnd = new Date(year, quarterIndex * 3 + 3, 0);
          quarterEnd.setHours(23, 59, 59, 999);

          // Set the milestone due date to the end of the quarter
          minor.milestone.dueOn = quarterEnd;
          minor.milestone.isEstimated = true;
          scheduled.push(minor.milestone);

          // Move to next quarter for next minor in this series
          minorQuarterCursor.setMonth(minorQuarterCursor.getMonth() + 3);
        }
      }
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
      const dueDate = new Date(milestone.dueOn);
      return dueDate.getTime() >= viewStartTime && dueDate.getTime() <= viewEndTime;
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
}
