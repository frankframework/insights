import { Component, Input, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue, IssuePriority } from '../../../services/issue.service';
import { GitHubState, GitHubStates } from '../../../app.service';

interface PositionedIssue {
  issue: Issue;
  style: Record<string, string>;
  track: number;
  startTime: number;
  endTime: number;
}

interface PlanningWindow {
  start: number;
  end: number;
}

interface LayoutContext {
  positionedIssues: PositionedIssue[];
  trackCount: number;
}

@Component({
  selector: 'app-milestone-row',
  standalone: true,
  imports: [CommonModule, IssueBarComponent, DatePipe],
  templateUrl: './milestone-row.component.html',
  styleUrls: ['./milestone-row.component.scss'],
})
export class MilestoneRowComponent implements OnInit {
  @Input({ required: true }) milestone!: Milestone;
  @Input({ required: true }) issues: Issue[] = [];
  @Input({ required: true }) timelineStartDate!: Date;
  @Input({ required: true }) totalTimelineDays!: number;
  @Input() isLast = false;

  public positionedIssues: PositionedIssue[] = [];
  public trackCount = 1;
  public progressPercentage = 0;

  private readonly DEFAULT_POINTS = 3;
  private readonly MIN_ISSUE_WIDTH_PERCENTAGE = 3;
  private readonly GAP_MS = 24 * 3600 * 1000;

  ngOnInit(): void {
    this.calculateProgress();
    if (this.milestone.dueOn) {
      this.runLayoutAlgorithm();
    }
  }

  public getIssuesForTrack(trackNumber: number): PositionedIssue[] {
    return this.positionedIssues.filter((p) => p.track === trackNumber);
  }

  public getTracks(): number[] {
    return Array.from({ length: this.trackCount }, (_, index) => index);
  }

  private runLayoutAlgorithm(): void {
    this.positionedIssues = [];

    if (this.isMilestoneOverdue()) {
      this.calculateLayoutForOverdueMilestone();
    } else if (this.isMilestoneInFuture()) {
      this.calculateLayoutForFutureMilestone();
    } else {
      this.calculateLayoutForCurrentMilestone();
    }

    const maxTrack = this.positionedIssues.reduce((max, p) => Math.max(max, p.track), -1);
    this.trackCount = Math.max(1, maxTrack + 1);
  }

  private calculateLayoutForOverdueMilestone(): void {
    const { closedIssues, openIssues } = this.getSeparatedIssues();
    const { currentViewClosedWindow } = this.getTimelineViewWindows();
    const nextQuarterWindow = this.getNextQuarterWindow();

    const closedLayout = this.layoutIssuesWithEvenSpacing(closedIssues, currentViewClosedWindow);
    const openLayout = this.layoutIssuesWithEvenSpacing(openIssues, nextQuarterWindow);

    this.positionedIssues.push(...closedLayout.positionedIssues, ...openLayout.positionedIssues);
  }

  private calculateLayoutForFutureMilestone(): void {
    const { closedIssues, openIssues } = this.getSeparatedIssues();
    const { currentViewClosedWindow } = this.getTimelineViewWindows();
    const milestoneQuarterWindow = this.getMilestoneQuarterWindow();

    const closedLayout = this.layoutIssuesBackwards(closedIssues, currentViewClosedWindow);
    const openLayout = this.layoutIssuesWithEvenSpacing(openIssues, milestoneQuarterWindow);

    this.positionedIssues.push(...closedLayout.positionedIssues, ...openLayout.positionedIssues);
  }

  private calculateLayoutForCurrentMilestone(): void {
    const { closedIssues, openIssues } = this.getSeparatedIssues();
    const { currentViewClosedWindow, currentViewOpenWindow } = this.getTimelineViewWindows();

    const closedLayout = this.layoutIssuesWithEvenSpacing(closedIssues, currentViewClosedWindow);
    const openLayout = this.layoutIssuesAsColumn(openIssues, currentViewOpenWindow);

    this.positionedIssues.push(...closedLayout.positionedIssues, ...openLayout.positionedIssues);
  }

  private layoutIssuesAsColumn(issues: Issue[], window: PlanningWindow): LayoutContext {
    const positionedIssues: PositionedIssue[] = [];
    const windowDuration = window.end - window.start;

    for (const [index, issue] of issues.entries()) {
      const durationMs = Math.min(
        this.getIssueDurationMsWithMinWidth(issue),
        windowDuration > 0 ? windowDuration : this.getIssueDurationMsWithMinWidth(issue),
      );
      positionedIssues.push(this.createPositionedIssue(issue, window.start, durationMs, index));
    }
    return { positionedIssues, trackCount: issues.length };
  }

  private layoutIssuesWithEvenSpacing(issues: Issue[], window: PlanningWindow): LayoutContext {
    if (issues.length === 0 || window.start >= window.end) return { positionedIssues: [], trackCount: 0 };

    const { issuesByTrack, trackCount } = this.assignIssuesToTracks(issues, window);
    const positionedIssues: PositionedIssue[] = [];

    for (const [trackIndex, trackIssues] of issuesByTrack.entries()) {
      const totalIssueDuration = trackIssues.reduce(
        (sum, issue) => sum + this.getIssueDurationMsWithMinWidth(issue),
        0,
      );
      const totalWhitespace = window.end - window.start - totalIssueDuration;
      const gapSize = totalWhitespace / (trackIssues.length + 1);
      let cursor = window.start + gapSize;

      for (const issue of trackIssues) {
        const durationMs = this.getIssueDurationMsWithMinWidth(issue);
        positionedIssues.push(this.createPositionedIssue(issue, cursor, durationMs, trackIndex));
        cursor += durationMs + gapSize;
      }
    }
    return { positionedIssues, trackCount };
  }

  private layoutIssuesBackwards(issues: Issue[], window: PlanningWindow): LayoutContext {
    return this.layoutIssuesWithEvenSpacing(issues, window);
  }

  private assignIssuesToTracks(
    issues: Issue[],
    window: PlanningWindow,
  ): { issuesByTrack: Map<number, Issue[]>; trackCount: number } {
    const issuesByTrack = new Map<number, Issue[]>();
    const trackEndTimes: number[] = [];

    for (const issue of issues) {
      const durationMs = this.getIssueDurationMsWithMinWidth(issue);
      const wasPlaced = this.tryPlacingOnExistingTrack(issue, durationMs, trackEndTimes, issuesByTrack, window);

      if (!wasPlaced) {
        this.placeOnNewTrack(issue, durationMs, trackEndTimes, issuesByTrack, window);
      }
    }
    return { issuesByTrack, trackCount: trackEndTimes.length };
  }

  private tryPlacingOnExistingTrack(
    issue: Issue,
    durationMs: number,
    trackEndTimes: number[],
    issuesByTrack: Map<number, Issue[]>,
    window: PlanningWindow,
  ): boolean {
    for (let index = 0; index < trackEndTimes.length; index++) {
      const lastEndTime = trackEndTimes[index] || window.start;
      if (lastEndTime + this.GAP_MS + durationMs <= window.end) {
        if (!issuesByTrack.has(index)) issuesByTrack.set(index, []);
        issuesByTrack.get(index)!.push(issue);
        trackEndTimes[index] = lastEndTime + this.GAP_MS + durationMs;
        return true;
      }
    }
    return false;
  }

  private placeOnNewTrack(
    issue: Issue,
    durationMs: number,
    trackEndTimes: number[],
    issuesByTrack: Map<number, Issue[]>,
    window: PlanningWindow,
  ): void {
    if (window.start + durationMs <= window.end) {
      const newTrackIndex = trackEndTimes.length;
      if (!issuesByTrack.has(newTrackIndex)) issuesByTrack.set(newTrackIndex, []);
      issuesByTrack.get(newTrackIndex)!.push(issue);
      trackEndTimes.push(window.start + durationMs);
    }
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
      startTime: startTime,
      endTime: startTime + durationMs,
    };
  }

  private calculateProgress(): void {
    const total = this.milestone.openIssueCount + this.milestone.closedIssueCount;
    this.progressPercentage = total === 0 ? 0 : Math.round((this.milestone.closedIssueCount / total) * 100);
  }

  private getSeparatedIssues(): { closedIssues: Issue[]; openIssues: Issue[] } {
    return {
      closedIssues: this.getSortedIssues(GitHubStates.CLOSED),
      openIssues: this.getSortedIssues(GitHubStates.OPEN),
    };
  }

  private isMilestoneOverdue(): boolean {
    if (!this.milestone.dueOn) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.milestone.dueOn.getTime() < today.getTime();
  }

  private isMilestoneInFuture(): boolean {
    if (!this.milestone.dueOn) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const quarterStartDate = this.getMilestoneQuarterWindow().start;
    return quarterStartDate > today.getTime();
  }

  private getNextQuarterWindow(): PlanningWindow {
    const lastDueDate = this.milestone.dueOn!;
    const nextMonth = new Date(lastDueDate.getFullYear(), lastDueDate.getMonth() + 1, 1);
    const nextQuarterIndex = Math.floor(nextMonth.getMonth() / 3);
    const nextQuarterYear = nextMonth.getFullYear();
    const startDate = new Date(nextQuarterYear, nextQuarterIndex * 3, 1);
    const endDate = new Date(nextQuarterYear, nextQuarterIndex * 3 + 3, 0);
    endDate.setHours(23, 59, 59, 999);
    return { start: startDate.getTime(), end: endDate.getTime() };
  }

  private getTimelineViewWindows(): { currentViewClosedWindow: PlanningWindow; currentViewOpenWindow: PlanningWindow } {
    const today = new Date();
    const timelineStartMs = this.timelineStartDate.getTime();
    const timelineEndDate = new Date(this.timelineStartDate);
    timelineEndDate.setDate(timelineEndDate.getDate() + this.totalTimelineDays);
    const timelineEndMs = timelineEndDate.getTime();
    const midpoint = new Date(Math.max(timelineStartMs, Math.min(today.getTime(), timelineEndMs)));
    return {
      currentViewClosedWindow: { start: timelineStartMs, end: midpoint.getTime() },
      currentViewOpenWindow: { start: midpoint.getTime(), end: timelineEndMs },
    };
  }

  private getMilestoneQuarterWindow(): PlanningWindow {
    const due = new Date(this.milestone.dueOn!);
    const year = due.getFullYear();
    const quarterIndex = Math.floor(due.getMonth() / 3);
    const startDate = new Date(year, quarterIndex * 3, 1);
    const endDate = new Date(year, quarterIndex * 3 + 3, 0);
    endDate.setHours(23, 59, 59, 999);
    return { start: startDate.getTime(), end: endDate.getTime() };
  }

  private calculateBarPosition(startDate: Date, durationDays: number): Record<string, string> {
    const startDays = (startDate.getTime() - this.timelineStartDate.getTime()) / (1000 * 3600 * 24);
    const leftPercentage = (startDays / this.totalTimelineDays) * 100;
    const durationMs = durationDays * 24 * 60 * 60 * 1000;
    const issueWidthPercentage = (durationMs / (this.totalTimelineDays * 24 * 60 * 60 * 1000)) * 100;
    return {
      left: `${leftPercentage}%`,
      width: `${Math.max(issueWidthPercentage, this.MIN_ISSUE_WIDTH_PERCENTAGE)}%`,
    };
  }

  private getIssueDurationMs(issue: Issue): number {
    const points = issue.points ?? this.DEFAULT_POINTS;
    return points * 24 * 60 * 60 * 1000;
  }

  private getIssueDurationMsWithMinWidth(issue: Issue): number {
    const durationMs = this.getIssueDurationMs(issue);
    const minDurationInMs = this.totalTimelineDays * (this.MIN_ISSUE_WIDTH_PERCENTAGE / 100) * 24 * 60 * 60 * 1000;
    return Math.max(durationMs, minDurationInMs);
  }

  private getSortedIssues(state: GitHubState): Issue[] {
    const issues = this.issues.filter((issue) => issue.state === state);
    const priorityOrder: Record<string, number> = { critical: 1, high: 2, medium: 3, low: 4, no: 5 };
    return [...issues].sort((a, b) => {
      const priorityA = priorityOrder[this.getPriorityKey(a.issuePriority)] ?? 5;
      const priorityB = priorityOrder[this.getPriorityKey(b.issuePriority)] ?? 5;
      if (priorityA !== priorityB) return priorityA - priorityB;
      const pointsA = a.points ?? this.DEFAULT_POINTS;
      const pointsB = b.points ?? this.DEFAULT_POINTS;
      if (pointsA !== pointsB) return pointsB - pointsA;
      return b.number - a.number;
    });
  }

  private getPriorityKey(priority: IssuePriority | undefined | null): string {
    if (!priority?.name) return 'no';
    const lowerCaseName = priority.name.toLowerCase();
    const keys = ['critical', 'high', 'medium', 'low'];
    for (const key of keys) {
      if (lowerCaseName.includes(key)) return key;
    }
    return 'no';
  }
}
