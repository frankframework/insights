import { Component, inject, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';
import { ReleaseRoadmapComponent, ViewMode } from '../release-roadmap.component';

interface PositionedIssue {
  issue?: Issue;
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

  private readonly UNPLANNED_EPICS_ID = 'unplanned-epics';
  private readonly DEFAULT_POINTS = 3;
  private readonly MIN_ISSUE_WIDTH_PERCENTAGE = 3;
  private readonly GAP_MS = 12 * 3600 * 1000;
  private readonly EPIC_POINTS = 30;
  private readonly MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  private releaseRoadmapComponent = inject(ReleaseRoadmapComponent);

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

  public isUnplannedEpicMilestone(): boolean {
    return this.isUnplannedEpic();
  }

  private runLayoutAlgorithm(): void {
    if (this.issues.length === 0) {
      this.positionedIssues = [];
      this.trackCount = 1;
      return;
    }

    if (this.isUnplannedEpic()) {
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
    const currentQuarterStart = this.getCurrentQuarterStart();

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
      return this.getMonthlyIssueKey(issue, currentQuarterStart);
    }
    return this.getQuarterlyIssueKey(issue, currentQuarterStart);
  }

  private getMonthlyIssueKey(issue: Issue, currentQuarterStart: Date): string {
    if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
      return this.getMonthKey(new Date(issue.closedAt));
    }

    const milestoneDueDate = this.milestone.dueOn ? new Date(this.milestone.dueOn) : new Date();
    const effectiveDate =
      milestoneDueDate.getTime() < currentQuarterStart.getTime() ? currentQuarterStart : milestoneDueDate;

    return this.getMonthKey(effectiveDate);
  }

  private getQuarterlyIssueKey(issue: Issue, currentQuarterStart: Date): string {
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
      return this.releaseRoadmapComponent.createQuarterWindow(quarterMatch);
    }

    const monthMatch = quarterKey.match(/(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) (\d{4})/);
    if (monthMatch) {
      return this.createMonthWindow(monthMatch);
    }

    return null;
  }

  private createMonthWindow(monthMatch: RegExpMatchArray): PlanningWindow {
    const monthIndex = this.MONTH_NAMES.indexOf(monthMatch[1]);
    const year = Number.parseInt(monthMatch[2], 10);
    const startDate = new Date(year, monthIndex, 1);
    const endDate = new Date(year, monthIndex + 1, 0);
    endDate.setHours(23, 59, 59, 999);
    return { start: startDate.getTime(), end: endDate.getTime() };
  }

  private getPlanningWindowsForQuarter(quarterWindow: PlanningWindow): {
    closedWindow: PlanningWindow;
    openWindow: PlanningWindow;
  } {
    const todayMs = this.getTodayEndOfDay();
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

      const issuesWithPositions = this.distributeIssuesInWindow(trackIssues, window);
      for (const { issue, startTime, durationMs } of issuesWithPositions) {
        positionedIssues.push(this.createPositionedIssue(issue, startTime, durationMs, trackIndex));
      }
    }
    return { positionedIssues, trackCount: issuesByTrack.size };
  }

  private distributeIssuesInWindow(
    trackIssues: Issue[],
    window: PlanningWindow,
  ): { issue: Issue; startTime: number; durationMs: number }[] {
    const totalIssueDuration = trackIssues.reduce((sum, issue) => sum + this.getIssueDurationMsWithMinWidth(issue), 0);

    const totalWhitespace = window.end - window.start - totalIssueDuration;
    const gapSize = totalWhitespace > 0 ? totalWhitespace / (trackIssues.length + 1) : 0;
    let cursor = window.start + gapSize;

    return trackIssues.map((issue) => {
      const durationMs = this.getIssueDurationMsWithMinWidth(issue);
      const startTime = cursor;
      cursor += durationMs + gapSize;
      return { issue, startTime, durationMs };
    });
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
      const spaceNeeded = this.getIssueDurationMsWithMinWidth(issue) + this.GAP_MS;
      const trackIndex = this.findAvailableTrack(trackCapacities, windowDurationMs, spaceNeeded, currentTrack);

      if (trackIndex === null) {
        currentTrack++;
        this.addIssueToNewTrack(issuesByTrack, trackCapacities, currentTrack, issue, spaceNeeded);
      } else {
        this.addIssueToExistingTrack(issuesByTrack, trackCapacities, trackIndex, issue, spaceNeeded);
      }
    }

    return issuesByTrack;
  }

  private findAvailableTrack(
    trackCapacities: Map<number, number>,
    windowDurationMs: number,
    spaceNeeded: number,
    currentTrack: number,
  ): number | null {
    for (let trackIndex = 0; trackIndex <= currentTrack; trackIndex++) {
      const trackUsed = trackCapacities.get(trackIndex) ?? 0;
      const trackRemaining = windowDurationMs - trackUsed;

      if (trackRemaining >= spaceNeeded) {
        return trackIndex;
      }
    }
    return null;
  }

  private addIssueToExistingTrack(
    issuesByTrack: Map<number, Issue[]>,
    trackCapacities: Map<number, number>,
    trackIndex: number,
    issue: Issue,
    spaceNeeded: number,
  ): void {
    if (!issuesByTrack.has(trackIndex)) {
      issuesByTrack.set(trackIndex, []);
    }
    issuesByTrack.get(trackIndex)!.push(issue);
    const trackUsed = trackCapacities.get(trackIndex) ?? 0;
    trackCapacities.set(trackIndex, trackUsed + spaceNeeded);
  }

  private addIssueToNewTrack(
    issuesByTrack: Map<number, Issue[]>,
    trackCapacities: Map<number, number>,
    trackIndex: number,
    issue: Issue,
    spaceNeeded: number,
  ): void {
    issuesByTrack.set(trackIndex, [issue]);
    trackCapacities.set(trackIndex, spaceNeeded);
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
    return this.releaseRoadmapComponent.getQuarterFromDate(date);
  }

  private getQuarterKey(quarter: Date): string {
    return this.releaseRoadmapComponent.getQuarterKey(quarter);
  }

  private getMonthKey(date: Date): string {
    const year = date.getFullYear();
    const month = date.getMonth();
    return `${this.MONTH_NAMES[month]} ${year}`;
  }

  private calculateProgress(): void {
    if (!this.milestone) {
      this.progressPercentage = 0;
      return;
    }
    const total = this.milestone.openIssueCount + this.milestone.closedIssueCount;
    this.progressPercentage = total === 0 ? 0 : Math.round((this.milestone.closedIssueCount / total) * 100);
  }

  private isUnplannedEpic(): boolean {
    return this.milestone.id === this.UNPLANNED_EPICS_ID;
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
    const timelineEndTime = this.timelineEndDate.getTime();

    const sortedIssues = this.sortIssuesByPriority(this.issues);

    for (const issue of sortedIssues) {
      const durationMs = this.EPIC_POINTS * 24 * 60 * 60 * 1000;

      if (currentTime >= timelineEndTime) {
        break;
      }

      this.positionedIssues.push(this.createPositionedIssue(issue, currentTime, durationMs, 0));
      currentTime += durationMs + this.GAP_MS;
    }
  }

  private layoutMonthlyViewForUnplannedEpics(): void {
    this.positionedIssues = [];

    if (!this.milestone.dueOn) return;

    const positions = new Map<Issue, { startTime: number; endTime: number }>();
    const startOffset = 6 * 60 * 60 * 1000;
    let currentTime = new Date(this.milestone.dueOn).getTime() + startOffset;

    const quarterStart = this.getQuarterFromDate(this.timelineStartDate);
    const quarterEnd = new Date(quarterStart);
    quarterEnd.setMonth(quarterEnd.getMonth() + 6);
    quarterEnd.setDate(0);
    quarterEnd.setHours(23, 59, 59, 999);
    const timelineEndTime = quarterEnd.getTime();

    const sortedIssues = this.sortIssuesByPriority(this.issues);

    for (const issue of sortedIssues) {
      const durationMs = this.EPIC_POINTS * 24 * 60 * 60 * 1000;

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
    const state = {
      trackIndex: 0,
      hasClosedIssues: false,
      hasAddedSeparator: false,
      hasAddedClosedLabel: false,
      hasAddedOpenLabel: false,
    };

    for (const issue of sortedIssues) {
      this.addLabelIfNeeded(issue, state);
      this.addIssueToTrack(issue, state.trackIndex, barStyle);
      state.trackIndex++;
    }

    return Math.max(1, state.trackIndex);
  }

  private addLabelIfNeeded(
    issue: Issue,
    state: {
      trackIndex: number;
      hasClosedIssues: boolean;
      hasAddedSeparator: boolean;
      hasAddedClosedLabel: boolean;
      hasAddedOpenLabel: boolean;
    },
  ): void {
    const isClosed = issue.state === GitHubStates.CLOSED;
    const isOpen = issue.state === GitHubStates.OPEN;

    if (isClosed) {
      this.addClosedLabelIfNeeded(state);
      state.hasClosedIssues = true;
    }

    if (isOpen) {
      this.addOpenLabelIfNeeded(state);
    }
  }

  private addClosedLabelIfNeeded(state: { trackIndex: number; hasAddedClosedLabel: boolean }): void {
    if (!state.hasAddedClosedLabel) {
      this.addLabel('Closed Issues', state.trackIndex);
      state.trackIndex++;
      state.hasAddedClosedLabel = true;
    }
  }

  private addOpenLabelIfNeeded(state: {
    trackIndex: number;
    hasClosedIssues: boolean;
    hasAddedSeparator: boolean;
    hasAddedOpenLabel: boolean;
  }): void {
    if (state.hasAddedOpenLabel) {
      return;
    }

    if (state.hasClosedIssues && !state.hasAddedSeparator) {
      this.addOpenLabelAfterSeparator(state);
    } else if (!state.hasClosedIssues) {
      this.addOpenLabel(state);
    }
  }

  private addOpenLabelAfterSeparator(state: {
    trackIndex: number;
    hasAddedSeparator: boolean;
    hasAddedOpenLabel: boolean;
  }): void {
    state.trackIndex++;
    state.hasAddedSeparator = true;
    this.addLabel('Open Issues', state.trackIndex);
    state.trackIndex++;
    state.hasAddedOpenLabel = true;
  }

  private addOpenLabel(state: { trackIndex: number; hasAddedOpenLabel: boolean }): void {
    this.addLabel('Open Issues', state.trackIndex);
    state.trackIndex++;
    state.hasAddedOpenLabel = true;
  }

  private addLabel(labelText: string, trackIndex: number): void {
    this.positionedIssues.push({
      track: trackIndex,
      style: {},
      isLabel: true,
      labelText,
    });
  }

  private addIssueToTrack(issue: Issue, trackIndex: number, barStyle: Record<string, string>): void {
    this.positionedIssues.push({
      issue,
      track: trackIndex,
      style: barStyle,
    });
  }

  private calculateQuarterlyPositions(): Map<Issue, { startTime: number; endTime: number }> {
    const positions = new Map<Issue, { startTime: number; endTime: number }>();
    const currentQuarterStart = this.getCurrentQuarterStart();

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
      const quarterKey = this.getQuarterlyIssueKey(issue, currentQuarterStart);
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

  private calculateIssuePositionsInWindow(
    issues: Issue[],
    window: PlanningWindow,
    positions: Map<Issue, { startTime: number; endTime: number }>,
  ): void {
    if (issues.length === 0 || window.start >= window.end) return;

    const issuesByTrack = this.distributeIssuesWithBinPacking(issues, window);

    for (const trackIssues of issuesByTrack.values()) {
      if (trackIssues.length === 0) continue;

      const issuesWithPositions = this.distributeIssuesInWindow(trackIssues, window);
      for (const { issue, startTime, durationMs } of issuesWithPositions) {
        positions.set(issue, {
          startTime,
          endTime: startTime + durationMs,
        });
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
      return this.compareIssues(a, b, false);
    });
  }

  private sortIssuesForMonthlyView(issues: Issue[]): Issue[] {
    return [...issues].sort((a, b) => {
      return this.compareIssues(a, b, true);
    });
  }

  private compareIssues(a: Issue, b: Issue, isMonthlyView: boolean): number {
    // Sort by state
    const stateComparison = this.compareIssueStates(a, b, isMonthlyView);
    if (stateComparison !== 0) return stateComparison;

    // Sort by priority
    const priorityComparison = this.compareIssuePriorities(a, b);
    if (priorityComparison !== 0) return priorityComparison;

    // Sort by points
    const pointsComparison = this.compareIssuePoints(a, b, isMonthlyView);
    if (pointsComparison !== 0) return pointsComparison;

    // Sort by issue number
    return a.number - b.number;
  }

  private compareIssueStates(a: Issue, b: Issue, closedFirst: boolean): number {
    if (a.state !== b.state) {
      const closedValue = closedFirst ? -1 : 1;
      if (a.state === GitHubStates.CLOSED) return closedValue;
      if (b.state === GitHubStates.CLOSED) return -closedValue;
    }
    return 0;
  }

  private compareIssuePriorities(a: Issue, b: Issue): number {
    const aPriorityOrder = this.getPriorityOrder(a.issuePriority?.name);
    const bPriorityOrder = this.getPriorityOrder(b.issuePriority?.name);
    return aPriorityOrder - bPriorityOrder;
  }

  private compareIssuePoints(a: Issue, b: Issue, descendingOrder: boolean): number {
    const aPoints = a.points ?? 0;
    const bPoints = b.points ?? 0;

    if (aPoints !== bPoints) {
      return descendingOrder ? bPoints - aPoints : aPoints - bPoints;
    }
    return 0;
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

  private getCurrentQuarterStart(): Date {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.getQuarterFromDate(today);
  }

  private getTodayEndOfDay(): number {
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    return today.getTime();
  }
}
