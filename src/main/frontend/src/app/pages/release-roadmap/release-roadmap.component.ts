import { ChangeDetectorRef, Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
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
import { RoadmapLegend } from './roadmap-legend/roadmap-legend.component';

export enum ViewMode {
  QUARTERLY = 'quarterly',
  MONTHLY = 'monthly',
}

interface Version {
  major: number;
  minor: number;
  patch: number;
  source: string;
}

export interface IssueStateStyle {
  [key: string]: string;
  'background-color': string;
  color: string;
  'border-color': string;
}

export const ISSUE_STATE_STYLES: Record<string, IssueStateStyle> = {
  Todo: {
    'background-color': '#f0fdf4',
    color: '#166534',
    'border-color': '#86efac',
  },
  'On hold': {
    'background-color': '#fee2e2',
    color: '#991b1b',
    'border-color': '#fca5a5',
  },
  'In Progress': {
    'background-color': '#dbeafe',
    color: '#1e3a8a',
    'border-color': '#93c5fd',
  },
  Review: {
    'background-color': '#fefce8',
    color: '#a16207',
    'border-color': '#fde047',
  },
  Done: {
    'background-color': '#f3e8ff',
    color: '#581c87',
    'border-color': '#d8b4fe',
  },
};

export const CLOSED_STYLE: IssueStateStyle = {
  'background-color': '#f3e8ff',
  color: '#581c87',
  'border-color': '#d8b4fe',
};

export const OPEN_STYLE: IssueStateStyle = {
  'background-color': '#f0fdf4',
  color: '#166534',
  'border-color': '#86efac',
};

@Component({
  selector: 'app-release-roadmap',
  standalone: true,
  imports: [
    CommonModule,
    RoadmapToolbarComponent,
    TimelineHeaderComponent,
    MilestoneRowComponent,
    LoaderComponent,
    RoadmapLegend,
  ],
  templateUrl: './release-roadmap.component.html',
  styleUrls: ['./release-roadmap.component.scss'],
})
export class ReleaseRoadmapComponent implements OnInit {
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLDivElement>;

  public isLoading = true;
  public milestones: Milestone[] = [];
  public milestoneIssues = new Map<string, Issue[]>();
  public unplannedEpics: Issue[] = [];
  public timelineStartDate!: Date;
  public timelineEndDate!: Date;
  public months: Date[] = [];
  public quarters: { name: string; monthCount: number }[] = [];
  public totalDays = 0;
  public displayDate!: Date;
  public currentPeriodLabel = '';
  public viewMode: ViewMode = ViewMode.QUARTERLY;
  protected todayOffsetPercentage = 0;

  private milestoneService = inject(MilestoneService);
  private issueService = inject(IssueService);
  private toastrService = inject(ToastrService);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.resetPeriod();
  }

  public changePeriod(months: number): void {
    this.displayDate.setMonth(this.displayDate.getMonth() + months);
    this.displayDate = new Date(this.displayDate);
    this.generateTimelineFromPeriod();
  }

  public resetPeriod(): void {
    const today = new Date();
    if (this.viewMode === ViewMode.QUARTERLY) {
      const currentQuarterStartMonth = Math.floor(today.getMonth() / 3) * 3;
      this.displayDate = new Date(today.getFullYear(), currentQuarterStartMonth, 1);
    } else {
      this.displayDate = new Date(today.getFullYear(), today.getMonth(), 1);
    }
    this.generateTimelineFromPeriod();
  }

  public toggleViewMode(): void {
    this.viewMode = this.viewMode === ViewMode.QUARTERLY ? ViewMode.MONTHLY : ViewMode.QUARTERLY;
    this.resetPeriod();
  }

  public getIssuesForMilestone(milestoneId: string): Issue[] {
    return this.milestoneIssues.get(milestoneId) || [];
  }

  public getVisibleMilestones(): Milestone[] {
    if (this.viewMode === ViewMode.QUARTERLY) {
      return this.milestones;
    }

    return this.milestones.filter((milestone) => {
      const issues = this.getIssuesForMilestone(milestone.id);
      if (issues.length === 0) return false;

      return this.milestoneHasIssuesInMonth(milestone, issues);
    });
  }

  private milestoneHasIssuesInMonth(milestone: Milestone, issues: Issue[]): boolean {
    if (milestone.id === 'unplanned-epics') {
      return this.hasUnplannedEpicsInMonth(milestone, issues);
    }

    const issuePositions = this.calculateIssuePositionsForMilestone(milestone, issues);

    const monthStart = this.timelineStartDate.getTime();
    const monthEnd = this.timelineEndDate.getTime();

    for (const position of issuePositions.values()) {
      if (position.startTime <= monthEnd && position.endTime >= monthStart) {
        return true;
      }
    }

    return false;
  }

  private calculateIssuePositionsForMilestone(
    milestone: Milestone,
    issues: Issue[],
  ): Map<Issue, { startTime: number; endTime: number }> {
    const positions = new Map<Issue, { startTime: number; endTime: number }>();

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const currentQuarterStart = this.getQuarterFromDate(today);

    const issuesByQuarter = this.groupIssuesByQuarter(issues, milestone, currentQuarterStart);

    for (const [quarterKey, quarterIssues] of issuesByQuarter.entries()) {
      const quarterWindow = this.getQuarterWindow(quarterKey);
      if (!quarterWindow) continue;

      const { closedWindow, openWindow } = this.splitQuarterWindow(quarterWindow);

      this.positionIssuesInWindow(quarterIssues.closed, closedWindow, positions);
      this.positionIssuesInWindow(quarterIssues.open, openWindow, positions);
    }

    return positions;
  }

  private groupIssuesByQuarter(
    issues: Issue[],
    milestone: Milestone,
    currentQuarterStart: Date,
  ): Map<string, { open: Issue[]; closed: Issue[] }> {
    const quarterMap = new Map<string, { open: Issue[]; closed: Issue[] }>();

    for (const issue of issues) {
      let quarterKey: string;

      if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
        const closedQuarter = this.getQuarterFromDate(new Date(issue.closedAt));
        quarterKey = this.getQuarterKey(closedQuarter);
      } else {
        const milestoneDueQuarter = milestone.dueOn
          ? this.getQuarterFromDate(new Date(milestone.dueOn))
          : currentQuarterStart;

        const effectiveQuarter =
          milestoneDueQuarter.getTime() < currentQuarterStart.getTime() ? currentQuarterStart : milestoneDueQuarter;

        quarterKey = this.getQuarterKey(effectiveQuarter);
      }

      if (!quarterMap.has(quarterKey)) {
        quarterMap.set(quarterKey, { open: [], closed: [] });
      }

      const quarterBin = quarterMap.get(quarterKey)!;
      if (issue.state === GitHubStates.CLOSED) {
        quarterBin.closed.push(issue);
      } else {
        quarterBin.open.push(issue);
      }
    }

    return quarterMap;
  }

  private getQuarterWindow(quarterKey: string): { start: number; end: number } | null {
    const quarterMatch = quarterKey.match(/Q(\d) (\d{4})/);
    if (!quarterMatch) return null;

    const quarterNumber = Number.parseInt(quarterMatch[1], 10);
    const year = Number.parseInt(quarterMatch[2], 10);
    const startDate = new Date(year, (quarterNumber - 1) * 3, 1);
    const endDate = new Date(year, quarterNumber * 3, 0);
    endDate.setHours(23, 59, 59, 999);

    return { start: startDate.getTime(), end: endDate.getTime() };
  }

  private splitQuarterWindow(quarterWindow: { start: number; end: number }): {
    closedWindow: { start: number; end: number };
    openWindow: { start: number; end: number };
  } {
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    const todayMs = today.getTime();
    const { start, end } = quarterWindow;

    if (todayMs < start) return { closedWindow: { start, end: start }, openWindow: { start, end } };
    if (todayMs > end) return { closedWindow: { start, end }, openWindow: { start: end, end } };

    return { closedWindow: { start, end: todayMs }, openWindow: { start: todayMs, end } };
  }

  private positionIssuesInWindow(
    issues: Issue[],
    window: { start: number; end: number },
    positions: Map<Issue, { startTime: number; endTime: number }>,
  ): void {
    if (issues.length === 0 || window.start >= window.end) return;

    const DEFAULT_POINTS = 3;

    let totalDuration = 0;
    for (const issue of issues) {
      const points = issue.points ?? DEFAULT_POINTS;
      const durationMs = points * 24 * 60 * 60 * 1000;
      totalDuration += durationMs;
    }

    const windowDuration = window.end - window.start;
    const totalWhitespace = windowDuration - totalDuration;
    const gapSize = totalWhitespace > 0 ? totalWhitespace / (issues.length + 1) : 0;

    let cursor = window.start + gapSize;

    for (const issue of issues) {
      const points = issue.points ?? DEFAULT_POINTS;
      const durationMs = points * 24 * 60 * 60 * 1000;

      positions.set(issue, {
        startTime: cursor,
        endTime: cursor + durationMs,
      });

      cursor += durationMs + gapSize;
    }
  }

  private getQuarterKey(quarter: Date): string {
    const year = quarter.getFullYear();
    const quarterNumber = Math.floor(quarter.getMonth() / 3) + 1;
    return `Q${quarterNumber} ${year}`;
  }

  private hasUnplannedEpicsInMonth(milestone: Milestone, issues: Issue[]): boolean {
    if (!milestone.dueOn) return false;

    const monthStart = this.timelineStartDate.getTime();
    const monthEnd = this.timelineEndDate.getTime();

    const startOffset = 6 * 60 * 60 * 1000;
    const epicStartTime = new Date(milestone.dueOn).getTime() + startOffset;
    const EPIC_POINTS = 30;
    const GAP_MS = 12 * 3600 * 1000;
    const epicDuration = EPIC_POINTS * 24 * 60 * 60 * 1000;

    let currentTime = epicStartTime;
    for (const issue of issues) {
      const epicEndTime = currentTime + epicDuration;

      if (currentTime <= monthEnd && epicEndTime >= monthStart) {
        return true;
      }

      currentTime += epicDuration + GAP_MS;
    }

    return false;
  }

  private generateTimelineFromPeriod(): void {
    if (!this.displayDate) return;

    this.generateQuarters();
    this.calculateTodayMarkerPosition();
    this.loadRoadmapData();
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

    if (this.viewMode === ViewMode.QUARTERLY) {
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
    } else {
      const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const startYear = this.timelineStartDate.getFullYear();
      const startMonth = this.timelineStartDate.getMonth();
      const monthName = `${monthNames[startMonth]} ${startYear}`;
      this.quarters.push({ name: monthName, monthCount: 1 });

      this.currentPeriodLabel = monthName;
    }
  }

  private loadRoadmapData(): void {
    this.isLoading = true;
    forkJoin({
      milestones: this.milestoneService.getMilestones(),
      unplannedEpics: this.issueService.getFutureEpicIssues(),
    })
      .pipe(
        map(({ milestones, unplannedEpics }) => ({
          milestones: this.parseMilestones(milestones).filter((m) => m.dueOn !== null),
          unplannedEpics: unplannedEpics.sort((a, b) => a.number - b.number),
        })),
        switchMap(({ milestones, unplannedEpics }) =>
          this.fetchIssuesForMilestones(milestones).pipe(map((result) => ({ ...result, unplannedEpics }))),
        ),
        map(({ milestones, unplannedEpics }) => ({
          milestones: this.sortMilestones(milestones).filter((m) => this.hasIssuesInView(m)),
          unplannedEpics,
        })),
        tap(({ milestones, unplannedEpics }) => {
          this.milestones = milestones;
          this.unplannedEpics = unplannedEpics;
          this.addUnplannedEpicsMilestone();
        }),
        catchError((error) => {
          this.toastrService.error('Could not load roadmap data.', 'Error');
          console.error(error);
          return of(null);
        }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe();
  }

  private fetchIssuesForMilestones(milestones: Milestone[]): Observable<{ milestones: Milestone[] }> {
    if (milestones.length === 0) {
      this.milestones = [];
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

  private addUnplannedEpicsMilestone(): void {
    if (this.unplannedEpics.length === 0) return;

    const nextQuarterStart = this.getNextQuarterStart();
    const unplannedMilestone: Milestone = {
      id: 'unplanned-epics',
      number: 0,
      title: 'Unplanned Epics',
      url: '',
      state: GitHubStates.OPEN,
      dueOn: nextQuarterStart,
      openIssueCount: this.unplannedEpics.length,
      closedIssueCount: 0,
      isEstimated: false,
    };

    this.milestoneIssues.set('unplanned-epics', this.unplannedEpics);
    this.milestones.push(unplannedMilestone);
  }

  private getNextQuarterStart(): Date {
    const today = new Date();
    const currentQuarterStartMonth = Math.floor(today.getMonth() / 3) * 3;
    return new Date(today.getFullYear(), currentQuarterStartMonth + 3, 1);
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

  private parseMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.map((m) => ({ ...m, dueOn: m.dueOn ? new Date(m.dueOn) : null }));
  }

  private sortMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.sort((a, b) => {
      const versionComparison = this.compareMilestonesByVersion(a, b);
      if (versionComparison !== 0) {
        return versionComparison;
      }

      return a.title.localeCompare(b.title);
    });
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
    // Sort descending by major, then minor, then patch
    if (aVersion.major !== bVersion.major) return bVersion.major - aVersion.major;
    if (aVersion.minor !== bVersion.minor) return bVersion.minor - aVersion.minor;
    return bVersion.patch - aVersion.patch;
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
