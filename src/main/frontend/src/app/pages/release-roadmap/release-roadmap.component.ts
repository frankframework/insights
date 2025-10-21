import { ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
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
import { RoadmapFutureOffCanvasComponent } from './roadmap-future-off-canvas/roadmap-future-off-canvas';
import { RoadmapLegend } from './roadmap-legend/roadmap-legend.component';

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
    RoadmapFutureOffCanvasComponent,
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
  public timelineStartDate!: Date;
  public timelineEndDate!: Date;
  public months: Date[] = [];
  public quarters: { name: string; monthCount: number }[] = [];
  public totalDays = 0;
  public displayDate!: Date;
  public currentPeriodLabel = '';
  public isOffCanvasVisible = false;

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
      .getMilestones()
      .pipe(
        map((apiMilestones) => this.parseMilestones(apiMilestones)),
        map((parsedMilestones) => parsedMilestones.filter((m) => m.dueOn !== null)),
        switchMap((parsedMilestones) => this.fetchIssuesForMilestones(parsedMilestones)),
        map(({ milestones }) => this.sortMilestones(milestones)),
        map((sortedMilestones) => sortedMilestones.filter((m) => this.hasIssuesInView(m))),
        tap((finalMilestones) => (this.milestones = finalMilestones)),
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
