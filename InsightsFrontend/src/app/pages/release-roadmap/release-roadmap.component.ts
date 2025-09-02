import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, finalize, forkJoin, map, Observable, of, switchMap, tap } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { RoadmapToolbarComponent } from './roadmap-toolbar/roadmap-toolbar.component';
import { TimelineHeaderComponent } from './timeline-header/timeline-header.component';
import { MilestoneRowComponent } from './milestone-row/milestone-row.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { Issue, IssueService } from '../../services/issue.service';
import { Milestone, MilestoneService } from '../../services/milestone.service';
import { GitHubStates } from '../../app.service';
import { RoadmapFutureOffCanvasComponent } from './roadmap-toolbar/roadmap-future-off-canvas/roadmap-future-off-canvas';

interface Version {
  major: number;
  minor: number;
  patch: number;
  source: string;
}

interface MilestoneWithVersion {
  milestone: Milestone;
  version: Version | null;
}

@Component({
  selector: 'app-release-roadmap',
  standalone: true,
  imports: [
    CommonModule,
    RoadmapToolbarComponent,
    TimelineHeaderComponent,
    MilestoneRowComponent,
    LoaderComponent,
    RoadmapFutureOffCanvasComponent,
  ],
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
  public isOffCanvasVisible = false;

  protected todayOffsetPercentage = 0;

  private viewInitialized = false;
  private milestoneService = inject(MilestoneService);
  private issueService = inject(IssueService);
  private toastrService = inject(ToastrService);
  private cdr = inject(ChangeDetectorRef);

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

  public getIssuesForMilestone(milestoneId: string): Issue[] {
    return this.milestoneIssues.get(milestoneId) || [];
  }

  public openOffCanvas(): void {
    this.isOffCanvasVisible = true;
    this.cdr.markForCheck();
  }

  public closeOffCanvas(): void {
    this.isOffCanvasVisible = false;
    this.cdr.markForCheck();
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
        switchMap((parsedMilestones) => this.fetchIssuesForMilestones(parsedMilestones)),
        map(({ milestones }) => this.scheduleMilestones(milestones)),
        map((finalMilestones) => this.sortMilestones(finalMilestones)),
        map((sortedMilestones) => sortedMilestones.filter((m) => this.hasIssuesInView(m))),
        tap((finalMilestones) => (this.openMilestones = finalMilestones)),
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

  private fetchIssuesForMilestones(milestones: Milestone[]): Observable<{ milestones: Milestone[] }> {
    if (milestones.length === 0) {
      this.openMilestones = [];
      return of({ milestones: [] });
    }

    const issueRequests = milestones.map((m) =>
      this.issueService.getIssuesByMilestoneId(m.id).pipe(
        map((issues) => ({ milestoneId: m.id, issues })),
        catchError(() => of({ milestoneId: m.id, issues: [] })),
      ),
    );

    return forkJoin(issueRequests).pipe(
      map((issueResults) => {
        this.milestoneIssues.clear();
        for (const result of issueResults) this.milestoneIssues.set(result.milestoneId, result.issues);
        return { milestones };
      }),
    );
  }

  private scheduleMilestones(milestones: Milestone[]): Milestone[] {
    const milestonesWithVersions = this.getMilestonesWithParsedVersions(milestones);

    const majors = this.getMajorReleases(milestonesWithVersions);
    const minors = this.getMinorReleases(milestonesWithVersions);

    const majorQuarterMap = this.planMajorReleaseQuarters(majors);
    this.assignDueDatesToMajors(majors, majorQuarterMap);

    this.planAndAssignDueDatesToMinors(minors, majorQuarterMap);

    const uniqueMilestones = new Map<string, Milestone>();
    for (const { milestone, version } of [...majors, ...minors]) {
      if (version) {
        uniqueMilestones.set(`${version.major}.${version.minor}.${version.patch}`, milestone);
      }
    }

    return [...uniqueMilestones.values()];
  }

  private getMilestonesWithParsedVersions(milestones: Milestone[]): MilestoneWithVersion[] {
    return milestones
      .map((milestone) => ({ milestone, version: this.parseVersion(milestone.title) }))
      .filter((mv) => mv.version !== null);
  }

  private getMajorReleases(milestones: MilestoneWithVersion[]): MilestoneWithVersion[] {
    return milestones
      .filter((mv) => mv.version!.patch === 0)
      .sort((a, b) => {
        if (a.version!.major !== b.version!.major) return a.version!.major - b.version!.major;
        return a.version!.minor - b.version!.minor;
      });
  }

  private getMinorReleases(milestones: MilestoneWithVersion[]): MilestoneWithVersion[] {
    return milestones.filter((mv) => mv.version!.patch > 0);
  }

  private planMajorReleaseQuarters(majors: MilestoneWithVersion[]): Map<string, Date> {
    const majorQuarterMap = new Map<string, Date>();
    const today = new Date();
    const currentQuarter = this.getQuarterFromDate(today);

    for (const { milestone, version } of majors) {
      if (milestone.dueOn) {
        const quarter = this.getQuarterFromDate(new Date(milestone.dueOn));
        majorQuarterMap.set(`${version!.major}.${version!.minor}`, quarter);
      }
    }

    for (let index = 0; index < majors.length; index++) {
      const { version } = majors[index];
      const key = `${version!.major}.${version!.minor}`;

      if (!majorQuarterMap.has(key)) {
        let referenceQuarter: Date;
        const previousMajor = majors[index - 1];
        if (previousMajor) {
          const previousKey = `${previousMajor.version!.major}.${previousMajor.version!.minor}`;
          const previousQuarter = majorQuarterMap.get(previousKey)!;
          referenceQuarter = this.addQuarters(previousQuarter, 1);
        } else {
          referenceQuarter = currentQuarter;
        }
        majorQuarterMap.set(key, referenceQuarter);
      }
    }
    return majorQuarterMap;
  }

  private assignDueDatesToMajors(majors: MilestoneWithVersion[], majorQuarterMap: Map<string, Date>): void {
    for (const { milestone, version } of majors) {
      const key = `${version!.major}.${version!.minor}`;
      const quarter = majorQuarterMap.get(key)!;
      const hadOriginalDueDate = !!milestone.dueOn;
      milestone.dueOn = this.getQuarterEndDate(quarter);
      milestone.isEstimated = !hadOriginalDueDate;
    }
  }

  private planAndAssignDueDatesToMinors(minors: MilestoneWithVersion[], majorQuarterMap: Map<string, Date>): void {
    const minorsByMajor = new Map<string, MilestoneWithVersion[]>();
    for (const mv of minors) {
      const key = `${mv.version!.major}.${mv.version!.minor}`;
      if (!minorsByMajor.has(key)) minorsByMajor.set(key, []);
      minorsByMajor.get(key)!.push(mv);
    }

    for (const [, minorList] of minorsByMajor) {
      minorList.sort((a, b) => a.version!.patch - b.version!.patch);

      const majorVersionKey = `${minorList[0].version!.major}.${minorList[0].version!.minor}`;
      let quarter = majorQuarterMap.get(majorVersionKey) || this.getQuarterFromDate(new Date());

      for (const minor of minorList) {
        minor.milestone.dueOn = this.getQuarterEndDate(quarter);
        minor.milestone.isEstimated = true;
        quarter = this.addQuarters(quarter, 1);
      }
    }
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
    if (issues.length === 0) return false;

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const currentQuarterStart = this.getQuarterFromDate(today);

    for (const issue of issues) {
      const issueQuarter = this.getIssueLayoutQuarter(issue, milestone, currentQuarterStart);
      if (!issueQuarter) continue;

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

  private getIssueLayoutQuarter(issue: Issue, milestone: Milestone, currentQuarterStart: Date): Date | null {
    if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
      return this.getQuarterFromDate(new Date(issue.closedAt));
    }

    if (issue.state === GitHubStates.OPEN) {
      const milestoneDueQuarter = milestone.dueOn
        ? this.getQuarterFromDate(new Date(milestone.dueOn))
        : currentQuarterStart;
      return milestoneDueQuarter.getTime() < currentQuarterStart.getTime() ? currentQuarterStart : milestoneDueQuarter;
    }

    return null;
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
      const dateComparison = this.compareMilestonesByDueDate(a, b);
      if (dateComparison !== 0) {
        return dateComparison;
      }

      const versionComparison = this.compareMilestonesByVersion(a, b);
      if (versionComparison !== 0) {
        return versionComparison;
      }

      return a.title.localeCompare(b.title);
    });
  }

  private compareMilestonesByDueDate(a: Milestone, b: Milestone): number {
    const aDue = a.dueOn ? new Date(a.dueOn).getTime() : -1;
    const bDue = b.dueOn ? new Date(b.dueOn).getTime() : -1;

    if (aDue === -1 && bDue !== -1) {
      return 1;
    }
    if (bDue === -1 && aDue !== -1) {
      return -1;
    }

    return aDue - bDue;
  }

  private compareMilestonesByVersion(a: Milestone, b: Milestone): number {
    const aVersion = this.parseVersion(a.title);
    const bVersion = this.parseVersion(b.title);

    if (aVersion && bVersion) {
      return this.compareMilestoneVersions(aVersion, bVersion);
    }

    return 0;
  }

  private compareMilestoneVersions(aVersion: Version, bVersion: Version): number {
    const aIsMajor = aVersion.patch === 0;
    const bIsMajor = bVersion.patch === 0;
    if (aIsMajor && !bIsMajor) return -1;
    if (!aIsMajor && bIsMajor) return 1;

    if (aVersion.major !== bVersion.major) return aVersion.major - bVersion.major;
    if (aVersion.minor !== bVersion.minor) return aVersion.minor - bVersion.minor;

    return aVersion.patch - bVersion.patch;
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

  private addQuarters(date: Date, quarters: number): Date {
    const newDate = new Date(date);
    newDate.setMonth(newDate.getMonth() + 3 * quarters);
    return newDate;
  }
}
