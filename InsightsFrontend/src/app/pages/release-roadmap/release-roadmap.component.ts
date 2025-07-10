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
import { IssueBarComponent } from './issue-bar/issue-bar.component';

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
  private scheduledMilestones = new Map<string, Date>();

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
    setTimeout(() => this.scrollToToday(), 0);
  }

  public changePeriod(months: number): void {
    this.displayDate.setMonth(this.displayDate.getMonth() + months);
    this.displayDate = new Date(this.displayDate);
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
        map(({ milestones }) => this.scheduleMilestones(milestones)),
        map((finalMilestones) => this.sortMilestones(finalMilestones)),
        map((sortedMilestones) => sortedMilestones.filter((m) => this.hasIssuesInView(m))),
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
          setTimeout(() => this.scrollToToday(), 0);
        }),
      )
      .subscribe();
  }

  private scheduleMilestones(milestones: Milestone[]): Milestone[] {
    const parsed = milestones
      .map((m) => ({ milestone: m, version: this.parseVersion(m.title) }))
      .filter((mv) => mv.version);

    const majors = parsed
      .filter((mv) => mv.version!.patch === 0)
      .sort((a, b) => {
        if (a.version!.major !== b.version!.major) return a.version!.major - b.version!.major;
        return a.version!.minor - b.version!.minor;
      });
    const minors = parsed.filter((mv) => mv.version!.patch > 0);

    const today = new Date();
    const currentQuarter = this.getQuarterFromDate(today);

    let majorQuarterMap = new Map<string, Date>();
    for (const { milestone, version } of majors) {
      if (!version) continue;
      if (milestone.dueOn) {
        const q = this.getQuarterFromDate(new Date(milestone.dueOn));
        majorQuarterMap.set(`${version.major}.${version.minor}`, q);
      }
    }
    for (let index = 0; index < majors.length; index++) {
      const { milestone, version } = majors[index];
      if (!version) continue;
      const key = `${version.major}.${version.minor}`;
      if (!majorQuarterMap.has(key)) {
        let nextWithDueOnIndex = -1;
        for (let index_ = index + 1; index_ < majors.length; index_++) {
          const nextVersion = majors[index_].version;
          if (!nextVersion) continue;
          const nextKey = `${nextVersion.major}.${nextVersion.minor}`;
          if (majorQuarterMap.has(nextKey)) {
            nextWithDueOnIndex = index_;
            break;
          }
        }
        if (nextWithDueOnIndex === -1) {
          let startQ: Date;
          if (majorQuarterMap.size > 0) {
            const lastPlanned = majors
              .slice(0, index)
              .reverse()
              .find((mv) => mv.version && majorQuarterMap.has(`${mv.version.major}.${mv.version.minor}`));
            if (lastPlanned && lastPlanned.version) {
              const lastQ = new Date(majorQuarterMap.get(`${lastPlanned.version.major}.${lastPlanned.version.minor}`)!);
              startQ = new Date(lastQ);
              startQ.setMonth(startQ.getMonth() + 3 * (index - majors.indexOf(lastPlanned)));
            } else {
              startQ = new Date(currentQuarter);
            }
          } else {
            startQ = new Date(currentQuarter);
          }
          majorQuarterMap.set(key, startQ);
        } else {
          const nextVersion = majors[nextWithDueOnIndex].version;
          if (!nextVersion) continue;
          let q = new Date(majorQuarterMap.get(`${nextVersion.major}.${nextVersion.minor}`)!);
          q.setMonth(q.getMonth() - 3 * (nextWithDueOnIndex - index));
          majorQuarterMap.set(key, q);
        }
      }
    }
    for (const { milestone, version } of majors) {
      if (!version) continue;
      const key = `${version.major}.${version.minor}`;
      const q = majorQuarterMap.get(key)!;
      milestone.dueOn = this.getQuarterEndDate(q);
      milestone.isEstimated = !milestone.dueOn;
    }

    let earliestMajorQuarter: Date | null = null;
    for (const { milestone, version } of majors) {
      if (!version) continue;
      if (milestone.dueOn) {
        const q = this.getQuarterFromDate(new Date(milestone.dueOn));
        if (!earliestMajorQuarter || q < earliestMajorQuarter) {
          earliestMajorQuarter = q;
        }
      }
    }
    if (!earliestMajorQuarter) {
      earliestMajorQuarter = new Date(currentQuarter);
    }

    const minorsByMajor = new Map<string, { milestone: Milestone; version: Version }[]>();
    for (const mv of minors) {
      const key = `${mv.version!.major}.${mv.version!.minor}`;
      if (!minorsByMajor.has(key)) minorsByMajor.set(key, []);
      minorsByMajor.get(key)!.push(mv as { milestone: Milestone; version: Version });
    }
    for (const [key, minorList] of minorsByMajor) {
      minorList.sort((a, b) => a.version.patch - b.version.patch);
      let lastMinorWithDueOnIndex = -1;
      for (let index = minorList.length - 1; index >= 0; index--) {
        if (minorList[index].milestone.dueOn) {
          lastMinorWithDueOnIndex = index;
          break;
        }
      }
      if (lastMinorWithDueOnIndex === -1) {
        let q = new Date(earliestMajorQuarter);
        for (const element of minorList) {
          element.milestone.dueOn = this.getQuarterEndDate(q);
          element.milestone.isEstimated = !element.milestone.dueOn;
          q.setMonth(q.getMonth() + 3);
        }
      } else {
        let q = this.getQuarterFromDate(new Date(minorList[lastMinorWithDueOnIndex].milestone.dueOn!));
        for (let index = lastMinorWithDueOnIndex; index >= 0; index--) {
          minorList[index].milestone.dueOn = this.getQuarterEndDate(q);
          minorList[index].milestone.isEstimated = !minorList[index].milestone.dueOn;
          q.setMonth(q.getMonth() - 3);
        }
        q = this.getQuarterFromDate(new Date(minorList[lastMinorWithDueOnIndex].milestone.dueOn!));
        for (let index = lastMinorWithDueOnIndex + 1; index < minorList.length; index++) {
          q.setMonth(q.getMonth() + 3);
          minorList[index].milestone.dueOn = this.getQuarterEndDate(q);
          minorList[index].milestone.isEstimated = !minorList[index].milestone.dueOn;
        }
      }
    }

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

  private hasIssuesInView(milestone: Milestone): boolean {
    const issues = this.milestoneIssues.get(milestone.id) || [];
    if (issues.length === 0) {
      return false;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const currentQuarterStart = this.getQuarterFromDate(today);

    for (const issue of issues) {
      let issueQuarter: Date;

      if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
        issueQuarter = this.getQuarterFromDate(new Date(issue.closedAt));
      } else if (issue.state === GitHubStates.OPEN) {
        const milestoneDueQuarter = milestone.dueOn
          ? this.getQuarterFromDate(new Date(milestone.dueOn))
          : currentQuarterStart;

        issueQuarter =
          milestoneDueQuarter.getTime() < currentQuarterStart.getTime() ? currentQuarterStart : milestoneDueQuarter;
      } else {
        continue;
      }

      const quarterEnd = this.getQuarterEndDate(issueQuarter);
      if (
        issueQuarter.getTime() <= this.timelineEndDate.getTime() &&
        quarterEnd.getTime() >= this.timelineStartDate.getTime()
      ) {
        return true;
      }
    }

    return false;
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
      const aDue = a.dueOn ? new Date(a.dueOn).getTime() : Number.MAX_SAFE_INTEGER;
      const bDue = b.dueOn ? new Date(b.dueOn).getTime() : Number.MAX_SAFE_INTEGER;
      if (aDue !== bDue) return aDue - bDue;

      const aMatch = a.title.match(/(\d+)\.(\d+)\.(\d+)/);
      const bMatch = b.title.match(/(\d+)\.(\d+)\.(\d+)/);
      if (aMatch && bMatch) {
        const aPatch = Number(aMatch[3]);
        const bPatch = Number(bMatch[3]);
        if (aPatch === 0 && bPatch > 0) return -1;
        if (aPatch > 0 && bPatch === 0) return 1;

        const aMajor = Number(aMatch[1]);
        const bMajor = Number(bMatch[1]);
        if (aMajor !== bMajor) return aMajor - bMajor;

        const aMinor = Number(aMatch[2]);
        const bMinor = Number(bMatch[2]);
        if (aMinor !== bMinor) return aMinor - bMinor;
        if (aPatch !== bPatch) return aPatch - bPatch;
      }

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
    quarterEnd.setDate(0);
    quarterEnd.setHours(23, 59, 59, 999);
    return quarterEnd;
  }
}
