import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';
import { ViewMode } from '../release-roadmap.component';

interface PositionedIssue {
  issue: Issue;
  style: Record<string, string>;
  track: number;
  isLabel?: boolean;
  labelText?: string;
}

interface PlanningWindow {
  start: number;
  end: number;
}

interface LayoutContext {
  positionedIssues: PositionedIssue[];
  trackCount: number;
}

type QuarterIssueMap = Map<string, { open: Issue[]; closed: Issue[] }>;

@Component({
  selector: 'app-milestone-row',
  standalone: true,
  imports: [CommonModule, IssueBarComponent, DatePipe],
  templateUrl: './milestone-row.component.html',
  styleUrls: ['./milestone-row.component.scss'],
})
export class MilestoneRowComponent implements OnChanges {
  @Input({ required: true }) milestone!: Milestone;
  @Input({ required: true }) issues: Issue[] = [];
  @Input({ required: true }) timelineStartDate!: Date;
  @Input({ required: true }) timelineEndDate!: Date;
  @Input({ required: true }) totalTimelineDays!: number;
  @Input({ required: true }) quarters!: { name: string; monthCount: number }[];
  @Input() isLast = false;
  @Input() viewMode: ViewMode = ViewMode.QUARTERLY;

  public positionedIssues: PositionedIssue[] = [];
  public trackCount = 1;
  public progressPercentage = 0;

  private readonly DEFAULT_POINTS = 3;
  private readonly MIN_ISSUE_WIDTH_PERCENTAGE = 3;
  private readonly GAP_MS = 12 * 3600 * 1000;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['milestone'] || changes['issues'] || changes['quarters'] || changes['viewMode']) {
      this.calculateProgress();
      this.runLayoutAlgorithm();
    }
  }

  public getIssuesForTrack(trackNumber: number): PositionedIssue[] {
    return this.positionedIssues.filter((p) => p.track === trackNumber);
  }

  public getTracks(): number[] {
    return Array.from({ length: this.trackCount }, (_, index) => index);
  }

  public getTrackAreaHeight(): number {
    if (this.viewMode === ViewMode.MONTHLY) {
      return this.trackCount * 2.5;
    }

    return this.trackCount * 2.25;
  }

  private runLayoutAlgorithm(): void {
    if (this.issues.length === 0) {
      this.positionedIssues = [];
      this.trackCount = 1;
      return;
    }

    if (this.milestone.id === 'unplanned-epics') {
      this.layoutUnplannedEpics();
      return;
    }

    if (this.viewMode === ViewMode.MONTHLY) {
      this.layoutMonthlyView();
      return;
    }

    this.positionedIssues = [];
    let overallMaxTrackCount = 0;

    const issuesByQuarter = this.distributeIssuesIntoQuarters();
    const visibleQuarters = new Set(this.quarters.map((q) => q.name));

    for (const [quarterKey, quarterIssues] of issuesByQuarter.entries()) {
      if (!visibleQuarters.has(quarterKey)) continue;

      const quarterWindow = this.getWindowForQuarter(quarterKey);
      if (!quarterWindow) continue;

      const { closedWindow, openWindow } = this.getPlanningWindowsForQuarter(quarterWindow);

      const sortedClosedIssues = this.sortIssuesByPriority(quarterIssues.closed);
      const sortedOpenIssues = this.sortIssuesByPriority(quarterIssues.open);

      const closedLayout = this.layoutIssuesWithEvenSpacing(sortedClosedIssues, closedWindow);
      const openLayout = this.layoutIssuesWithEvenSpacing(sortedOpenIssues, openWindow);

      this.positionedIssues.push(...closedLayout.positionedIssues, ...openLayout.positionedIssues);
      overallMaxTrackCount = Math.max(overallMaxTrackCount, closedLayout.trackCount, openLayout.trackCount);
    }

    this.trackCount = Math.max(1, overallMaxTrackCount);
  }

  private distributeIssuesIntoQuarters(): QuarterIssueMap {
    const quarterMap = this.initializeQuarterMap();
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const currentQuarterStart = this.getQuarterFromDate(today);

    for (const issue of this.issues) {
      const quarterKey = this.getIssueLayoutQuarterKey(issue, currentQuarterStart);
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

  private initializeQuarterMap(): QuarterIssueMap {
    const quarterMap: QuarterIssueMap = new Map();
    for (const q of this.quarters) {
      quarterMap.set(q.name, { open: [], closed: [] });
    }
    return quarterMap;
  }

  private getIssueLayoutQuarterKey(issue: Issue, currentQuarterStart: Date): string {
    if (this.viewMode === ViewMode.MONTHLY) {
      if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
        return this.getMonthKey(new Date(issue.closedAt));
      }

      const milestoneDueDate = this.milestone.dueOn ? new Date(this.milestone.dueOn) : new Date();
      const effectiveDate =
        milestoneDueDate.getTime() < currentQuarterStart.getTime() ? currentQuarterStart : milestoneDueDate;

      return this.getMonthKey(effectiveDate);
    }

    if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
      return this.getQuarterKey(this.getQuarterFromDate(new Date(issue.closedAt)));
    }

    const milestoneDueQuarter = this.milestone.dueOn
      ? this.getQuarterFromDate(new Date(this.milestone.dueOn))
      : currentQuarterStart;

    const effectiveQuarter =
      milestoneDueQuarter.getTime() < currentQuarterStart.getTime() ? currentQuarterStart : milestoneDueQuarter;

    return this.getQuarterKey(effectiveQuarter);
  }

  private getWindowForQuarter(quarterKey: string): PlanningWindow | null {
    const quarterMatch = quarterKey.match(/Q(\d) (\d{4})/);
    if (quarterMatch) {
      const quarterNumber = Number.parseInt(quarterMatch[1], 10);
      const year = Number.parseInt(quarterMatch[2], 10);
      const startDate = new Date(year, (quarterNumber - 1) * 3, 1);
      const endDate = new Date(year, quarterNumber * 3, 0);
      endDate.setHours(23, 59, 59, 999);
      return { start: startDate.getTime(), end: endDate.getTime() };
    }

    const monthMatch = quarterKey.match(/(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) (\d{4})/);
    if (monthMatch) {
      const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const monthIndex = monthNames.indexOf(monthMatch[1]);
      const year = Number.parseInt(monthMatch[2], 10);
      const startDate = new Date(year, monthIndex, 1);
      const endDate = new Date(year, monthIndex + 1, 0);
      endDate.setHours(23, 59, 59, 999);
      return { start: startDate.getTime(), end: endDate.getTime() };
    }

    return null;
  }

  private getPlanningWindowsForQuarter(quarterWindow: PlanningWindow): {
    closedWindow: PlanningWindow;
    openWindow: PlanningWindow;
  } {
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    const todayMs = today.getTime();
    const { start, end } = quarterWindow;

    if (todayMs < start) return { closedWindow: { start, end: start }, openWindow: { start, end } };
    if (todayMs > end) return { closedWindow: { start, end }, openWindow: { start: end, end } };

    return { closedWindow: { start, end: todayMs }, openWindow: { start: todayMs, end } };
  }

  private layoutIssuesWithEvenSpacing(issues: Issue[], window: PlanningWindow): LayoutContext {
    if (issues.length === 0 || window.start >= window.end) {
      return { positionedIssues: [], trackCount: 0 };
    }

    const issuesByTrack = this.distributeIssuesWithBinPacking(issues, window);
    const positionedIssues: PositionedIssue[] = [];

    for (const [trackIndex, trackIssues] of issuesByTrack.entries()) {
      if (trackIssues.length === 0) continue;

      const totalIssueDuration = trackIssues.reduce(
        (sum, issue) => sum + this.getIssueDurationMsWithMinWidth(issue),
        0,
      );

      const totalWhitespace = window.end - window.start - totalIssueDuration;
      const gapSize = totalWhitespace > 0 ? totalWhitespace / (trackIssues.length + 1) : 0;
      let cursor = window.start + gapSize;

      for (const issue of trackIssues) {
        const durationMs = this.getIssueDurationMsWithMinWidth(issue);
        positionedIssues.push(this.createPositionedIssue(issue, cursor, durationMs, trackIndex));
        cursor += durationMs + gapSize;
      }
    }
    return { positionedIssues, trackCount: issuesByTrack.size };
  }

  private createPositionedIssue(
    issue: Issue,
    startTime: number,
    durationMs: number,
    trackIndex: number,
  ): PositionedIssue {
    return {
      issue,
      track: trackIndex,
      style: this.calculateBarPosition(new Date(startTime), durationMs / (1000 * 3600 * 24)),
    };
  }

  private distributeIssuesWithBinPacking(issues: Issue[], window: PlanningWindow): Map<number, Issue[]> {
    const issuesByTrack = new Map<number, Issue[]>();
    const windowDurationMs = window.end - window.start;
    const trackCapacities = new Map<number, number>();
    let currentTrack = 0;

    for (const issue of issues) {
      const issueDuration = this.getIssueDurationMsWithMinWidth(issue);
      const spaceNeeded = issueDuration + this.GAP_MS;

      let placed = false;
      for (let trackIndex = 0; trackIndex <= currentTrack; trackIndex++) {
        const trackUsed = trackCapacities.get(trackIndex) ?? 0;
        const trackRemaining = windowDurationMs - trackUsed;

        if (trackRemaining >= spaceNeeded) {
          if (!issuesByTrack.has(trackIndex)) {
            issuesByTrack.set(trackIndex, []);
          }
          issuesByTrack.get(trackIndex)!.push(issue);
          trackCapacities.set(trackIndex, trackUsed + spaceNeeded);
          placed = true;
          break;
        }
      }

      if (!placed) {
        currentTrack++;
        issuesByTrack.set(currentTrack, [issue]);
        trackCapacities.set(currentTrack, spaceNeeded);
      }
    }

    return issuesByTrack;
  }

  private calculateBarPosition(startDate: Date, durationDays: number): Record<string, string> {
    const startDays = (startDate.getTime() - this.timelineStartDate.getTime()) / (1000 * 3600 * 24);
    const leftPercentage = (startDays / this.totalTimelineDays) * 100;
    const widthPercentage = (durationDays / this.totalTimelineDays) * 100;

    return {
      left: `${leftPercentage}%`,
      width: `${Math.max(widthPercentage, this.MIN_ISSUE_WIDTH_PERCENTAGE)}%`,
    };
  }

  private getIssueDurationMsWithMinWidth(issue: Issue): number {
    const points = issue.points ?? this.DEFAULT_POINTS;
    const durationMs = points * 24 * 60 * 60 * 1000;
    const minDurationMs = this.totalTimelineDays * (this.MIN_ISSUE_WIDTH_PERCENTAGE / 100) * (24 * 60 * 60 * 1000);
    return Math.max(durationMs, minDurationMs);
  }

  private getQuarterFromDate(date: Date): Date {
    const year = date.getFullYear();
    const quarterIndex = Math.floor(date.getMonth() / 3);
    return new Date(year, quarterIndex * 3, 1);
  }

  private getQuarterKey(quarter: Date): string {
    const year = quarter.getFullYear();
    const quarterNumber = Math.floor(quarter.getMonth() / 3) + 1;
    return `Q${quarterNumber} ${year}`;
  }

  private getMonthKey(date: Date): string {
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const year = date.getFullYear();
    const month = date.getMonth();
    return `${monthNames[month]} ${year}`;
  }

  private calculateProgress(): void {
    if (!this.milestone) {
      this.progressPercentage = 0;
      return;
    }
    const total = this.milestone.openIssueCount + this.milestone.closedIssueCount;
    this.progressPercentage = total === 0 ? 0 : Math.round((this.milestone.closedIssueCount / total) * 100);
  }

  private layoutUnplannedEpics(): void {
    this.positionedIssues = [];

    if (!this.milestone.dueOn) return;

    if (this.viewMode === ViewMode.MONTHLY) {
      this.layoutMonthlyViewForUnplannedEpics();
      return;
    }

    this.trackCount = 1;
    const startOffset = 6 * 60 * 60 * 1000;
    let currentTime = new Date(this.milestone.dueOn).getTime() + startOffset;
    const trackIndex = 0;
    const EPIC_POINTS = 30;
    const timelineEndTime = this.timelineEndDate.getTime();

    const sortedIssues = this.sortIssuesByPriority(this.issues);

    for (const issue of sortedIssues) {
      const durationMs = EPIC_POINTS * 24 * 60 * 60 * 1000;

      if (currentTime >= timelineEndTime) {
        break;
      }

      this.positionedIssues.push(this.createPositionedIssue(issue, currentTime, durationMs, trackIndex));
      currentTime += durationMs + this.GAP_MS;
    }
  }

  private layoutMonthlyViewForUnplannedEpics(): void {
    this.positionedIssues = [];

    if (!this.milestone.dueOn) return;

    const positions = new Map<Issue, { startTime: number; endTime: number }>();
    const startOffset = 6 * 60 * 60 * 1000;
    let currentTime = new Date(this.milestone.dueOn).getTime() + startOffset;
    const EPIC_POINTS = 30;

    const quarterStart = this.getQuarterFromDate(this.timelineStartDate);
    const quarterEnd = new Date(quarterStart);
    quarterEnd.setMonth(quarterEnd.getMonth() + 6);
    quarterEnd.setDate(0);
    quarterEnd.setHours(23, 59, 59, 999);
    const timelineEndTime = quarterEnd.getTime();

    const sortedIssues = this.sortIssuesByPriority(this.issues);

    for (const issue of sortedIssues) {
      const durationMs = EPIC_POINTS * 24 * 60 * 60 * 1000;

      if (currentTime >= timelineEndTime) {
        break;
      }

      positions.set(issue, {
        startTime: currentTime,
        endTime: currentTime + durationMs,
      });

      currentTime += durationMs + this.GAP_MS;
    }

    const filteredIssues = this.filterIssuesByMonthOverlap(positions);

    const sortedFilteredIssues = this.sortIssuesForMonthlyView(filteredIssues);

    this.trackCount = this.layoutIssuesWithLabels(sortedFilteredIssues, { left: '1%', width: '97%' });
  }

  private layoutMonthlyView(): void {
    this.positionedIssues = [];
    const quarterlyPositions = this.calculateQuarterlyPositions();
    const filteredIssues = this.filterIssuesByMonthOverlap(quarterlyPositions);
    const sortedIssues = this.sortIssuesForMonthlyView(filteredIssues);
    this.trackCount = this.layoutIssuesWithLabels(sortedIssues, { left: '1.75%', width: '96%' });
  }

  private layoutIssuesWithLabels(sortedIssues: Issue[], barStyle: Record<string, string>): number {
    let trackIndex = 0;
    let hasClosedIssues = false;
    let hasAddedSeparator = false;
    let hasAddedClosedLabel = false;
    let hasAddedOpenLabel = false;

    for (const issue of sortedIssues) {
      if (issue.state === GitHubStates.CLOSED && !hasAddedClosedLabel) {
        this.positionedIssues.push({
          issue: issue,
          track: trackIndex,
          style: {},
          isLabel: true,
          labelText: 'Closed Issues',
        });
        trackIndex++;
        hasAddedClosedLabel = true;
      }

      if (issue.state === GitHubStates.OPEN && hasClosedIssues && !hasAddedSeparator) {
        trackIndex++;
        hasAddedSeparator = true;

        if (!hasAddedOpenLabel) {
          this.positionedIssues.push({
            issue: issue,
            track: trackIndex,
            style: {},
            isLabel: true,
            labelText: 'Open Issues',
          });
          trackIndex++;
          hasAddedOpenLabel = true;
        }
      }

      if (issue.state === GitHubStates.OPEN && !hasClosedIssues && !hasAddedOpenLabel) {
        this.positionedIssues.push({
          issue: issue,
          track: trackIndex,
          style: {},
          isLabel: true,
          labelText: 'Open Issues',
        });
        trackIndex++;
        hasAddedOpenLabel = true;
      }

      if (issue.state === GitHubStates.CLOSED) {
        hasClosedIssues = true;
      }

      this.positionedIssues.push({
        issue,
        track: trackIndex,
        style: barStyle,
      });

      trackIndex++;
    }

    return Math.max(1, trackIndex);
  }

  private calculateQuarterlyPositions(): Map<Issue, { startTime: number; endTime: number }> {
    const positions = new Map<Issue, { startTime: number; endTime: number }>();

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const currentQuarterStart = this.getQuarterFromDate(today);

    const quarterStart = this.getQuarterFromDate(this.timelineStartDate);

    const temporaryQuarters: { name: string; monthCount: number }[] = [];
    const startYear = quarterStart.getFullYear();
    const startQuarterNumber = Math.floor(quarterStart.getMonth() / 3) + 1;
    const firstQuarterName = `Q${startQuarterNumber} ${startYear}`;
    temporaryQuarters.push({ name: firstQuarterName, monthCount: 3 });

    const nextQuarterStart = new Date(quarterStart);
    nextQuarterStart.setMonth(nextQuarterStart.getMonth() + 3);
    const endYear = nextQuarterStart.getFullYear();
    const endQuarterNumber = Math.floor(nextQuarterStart.getMonth() / 3) + 1;
    const secondQuarterName = `Q${endQuarterNumber} ${endYear}`;
    temporaryQuarters.push({ name: secondQuarterName, monthCount: 3 });

    const issuesByQuarter = this.distributeIssuesIntoQuartersForCalculation(currentQuarterStart);
    const visibleQuarters = new Set(temporaryQuarters.map((q) => q.name));

    for (const [quarterKey, quarterIssues] of issuesByQuarter.entries()) {
      if (!visibleQuarters.has(quarterKey)) continue;

      const quarterWindow = this.getWindowForQuarter(quarterKey);
      if (!quarterWindow) continue;

      const { closedWindow, openWindow } = this.getPlanningWindowsForQuarter(quarterWindow);

      const sortedClosedIssues = this.sortIssuesByPriority(quarterIssues.closed);
      const sortedOpenIssues = this.sortIssuesByPriority(quarterIssues.open);

      this.calculateIssuePositionsInWindow(sortedClosedIssues, closedWindow, positions);

      this.calculateIssuePositionsInWindow(sortedOpenIssues, openWindow, positions);
    }

    return positions;
  }

  private distributeIssuesIntoQuartersForCalculation(currentQuarterStart: Date): QuarterIssueMap {
    const quarterMap: QuarterIssueMap = new Map();

    const quarterStart = this.getQuarterFromDate(this.timelineStartDate);
    const quarters: { name: string; monthCount: number }[] = [];

    const startYear = quarterStart.getFullYear();
    const startQuarterNumber = Math.floor(quarterStart.getMonth() / 3) + 1;
    const firstQuarterName = `Q${startQuarterNumber} ${startYear}`;
    quarters.push({ name: firstQuarterName, monthCount: 3 });

    const nextQuarterStart = new Date(quarterStart);
    nextQuarterStart.setMonth(nextQuarterStart.getMonth() + 3);
    const endYear = nextQuarterStart.getFullYear();
    const endQuarterNumber = Math.floor(nextQuarterStart.getMonth() / 3) + 1;
    const secondQuarterName = `Q${endQuarterNumber} ${endYear}`;
    quarters.push({ name: secondQuarterName, monthCount: 3 });

    for (const q of quarters) {
      quarterMap.set(q.name, { open: [], closed: [] });
    }

    for (const issue of this.issues) {
      const quarterKey = this.getIssueLayoutQuarterKeyForCalculation(issue, currentQuarterStart);
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

  private getIssueLayoutQuarterKeyForCalculation(issue: Issue, currentQuarterStart: Date): string {
    if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
      return this.getQuarterKey(this.getQuarterFromDate(new Date(issue.closedAt)));
    }

    const milestoneDueQuarter = this.milestone.dueOn
      ? this.getQuarterFromDate(new Date(this.milestone.dueOn))
      : currentQuarterStart;

    const effectiveQuarter =
      milestoneDueQuarter.getTime() < currentQuarterStart.getTime() ? currentQuarterStart : milestoneDueQuarter;

    return this.getQuarterKey(effectiveQuarter);
  }

  private calculateIssuePositionsInWindow(
    issues: Issue[],
    window: PlanningWindow,
    positions: Map<Issue, { startTime: number; endTime: number }>,
  ): void {
    if (issues.length === 0 || window.start >= window.end) return;

    const issuesByTrack = this.distributeIssuesWithBinPacking(issues, window);

    for (const [trackIndex, trackIssues] of issuesByTrack.entries()) {
      if (trackIssues.length === 0) continue;

      const totalIssueDuration = trackIssues.reduce(
        (sum, issue) => sum + this.getIssueDurationMsWithMinWidth(issue),
        0,
      );

      const totalWhitespace = window.end - window.start - totalIssueDuration;
      const gapSize = totalWhitespace > 0 ? totalWhitespace / (trackIssues.length + 1) : 0;
      let cursor = window.start + gapSize;

      for (const issue of trackIssues) {
        const durationMs = this.getIssueDurationMsWithMinWidth(issue);
        positions.set(issue, {
          startTime: cursor,
          endTime: cursor + durationMs,
        });
        cursor += durationMs + gapSize;
      }
    }
  }

  private filterIssuesByMonthOverlap(positions: Map<Issue, { startTime: number; endTime: number }>): Issue[] {
    const monthStart = this.timelineStartDate.getTime();
    const monthEnd = this.timelineEndDate.getTime();

    return this.issues.filter((issue) => {
      const position = positions.get(issue);
      if (!position) return false;

      return position.startTime <= monthEnd && position.endTime >= monthStart;
    });
  }

  private sortIssuesByPriority(issues: Issue[]): Issue[] {
    return [...issues].sort((a, b) => {
      if (a.state !== b.state) {
        if (a.state === GitHubStates.CLOSED) return 1;
        if (b.state === GitHubStates.CLOSED) return -1;
      }

      const aPriorityOrder = this.getPriorityOrder(a.issuePriority?.name);
      const bPriorityOrder = this.getPriorityOrder(b.issuePriority?.name);

      if (aPriorityOrder !== bPriorityOrder) {
        return aPriorityOrder - bPriorityOrder;
      }

      const aPoints = a.points ?? 0;
      const bPoints = b.points ?? 0;

      if (aPoints !== bPoints) {
        return aPoints - bPoints;
      }

      return a.number - b.number;
    });
  }

  private sortIssuesForMonthlyView(issues: Issue[]): Issue[] {
    return [...issues].sort((a, b) => {
      if (a.state !== b.state) {
        if (a.state === GitHubStates.CLOSED) return -1;
        if (b.state === GitHubStates.CLOSED) return 1;
      }

      const aPriorityOrder = this.getPriorityOrder(a.issuePriority?.name);
      const bPriorityOrder = this.getPriorityOrder(b.issuePriority?.name);

      if (aPriorityOrder !== bPriorityOrder) {
        return aPriorityOrder - bPriorityOrder;
      }

      const aPoints = a.points ?? 0;
      const bPoints = b.points ?? 0;

      if (aPoints !== bPoints) {
        return bPoints - aPoints;
      }

      return a.number - b.number;
    });
  }

  private getPriorityOrder(priorityName: string | undefined): number {
    if (!priorityName) return 999;

    const nameLower = priorityName.toLowerCase();

    if (nameLower.includes('critical')) return 0;
    if (nameLower.includes('high')) return 1;
    if (nameLower.includes('medium')) return 2;
    if (nameLower.includes('low')) return 3;
    if (nameLower.includes('no')) return 4;

    return 999;
  }
}
