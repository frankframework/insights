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
}

interface TrackInfo {
  intervals: { start: number; end: number }[];
}

interface PlanningWindow {
  start: number;
  end: number;
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
  private readonly MIN_ISSUE_WIDTH_PERCENTAGE = 2.5;
  private readonly MINIMAL_GAP_MS = 60 * 60 * 1000;

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
    const closedIssues = this.getSortedIssues(GitHubStates.CLOSED);
    const openIssues = this.getSortedIssues(GitHubStates.OPEN);
    const finalPositionedIssues: PositionedIssue[] = [];
    const tracks: TrackInfo[] = [];

    if (this.isMilestoneInCurrentQuarter()) {
      this.runCurrentQuarterLayout(closedIssues, openIssues, tracks, finalPositionedIssues);
    } else {
      this.runPastOrFutureQuarterLayout(closedIssues, openIssues, tracks, finalPositionedIssues);
    }

    this.positionedIssues = finalPositionedIssues;
    this.trackCount = Math.max(1, tracks.length);
  }

  private runCurrentQuarterLayout(
    closedIssues: Issue[],
    openIssues: Issue[],
    tracks: TrackInfo[],
    positionedIssues: PositionedIssue[],
  ): void {
    const { closedWindow, openWindow } = this.getPlanningWindows();
    if (!closedWindow || !openWindow) return;

    this.distributeIssuesInWindow(closedIssues, closedWindow, tracks, positionedIssues);
    this.distributeIssuesInWindow(openIssues, openWindow, tracks, positionedIssues);
  }

  private runPastOrFutureQuarterLayout(
    closedIssues: Issue[],
    openIssues: Issue[],
    tracks: TrackInfo[],
    positionedIssues: PositionedIssue[],
  ): void {
    if (!this.milestone.dueOn) return;
    const fullQuarterWindow = this.getMilestoneQuarterWindow();
    this.layoutIssuesSequentially(closedIssues, openIssues, fullQuarterWindow, tracks, positionedIssues);
  }

  private distributeIssuesInWindow(
    issues: Issue[],
    window: PlanningWindow,
    tracks: TrackInfo[],
    positionedIssues: PositionedIssue[],
  ): void {
    if (issues.length === 0) return;

    const trackCount = this.estimateTrackCount(issues, window);
    const issuesByTrack = this.groupIssuesIntoTracks(issues, trackCount);

    for (const [trackIndex, issuesOnThisTrack] of issuesByTrack.entries()) {
      this.placeIssuesForSingleTrack(issuesOnThisTrack, window, trackIndex, tracks, positionedIssues);
    }
  }

  private layoutIssuesSequentially(
    group1: Issue[],
    group2: Issue[],
    window: PlanningWindow,
    tracks: TrackInfo[],
    positionedIssues: PositionedIssue[],
  ): void {
    const allIssues = [...group1, ...group2];
    if (allIssues.length === 0) return;

    const totalTrackCount = this.estimateTrackCount(allIssues, window);
    const issuesByTrack = this.groupIssuesIntoTracks(allIssues, totalTrackCount);
    const group1Set = new Set(group1);

    for (const [trackIndex, trackIssues] of issuesByTrack.entries()) {
      this.placeIssueGroupsOnTrack(trackIssues, group1Set, window, trackIndex, tracks, positionedIssues);
    }
  }

  private placeIssueGroupsOnTrack(
    trackIssues: Issue[],
    group1Set: Set<Issue>,
    window: PlanningWindow,
    trackIndex: number,
    tracks: TrackInfo[],
    positionedIssues: PositionedIssue[],
  ): void {
    const group1IssuesOnTrack = trackIssues.filter((index) => group1Set.has(index));
    const group2IssuesOnTrack = trackIssues.filter((index) => !group1Set.has(index));

    const { window1, window2 } = this.getProportionalWindowsForTrack(group1IssuesOnTrack, group2IssuesOnTrack, window);

    this.placeIssuesForSingleTrack(group1IssuesOnTrack, window1, trackIndex, tracks, positionedIssues);
    this.placeIssuesForSingleTrack(group2IssuesOnTrack, window2, trackIndex, tracks, positionedIssues);
  }

  private placeIssuesForSingleTrack(
    issuesOnTrack: Issue[],
    window: PlanningWindow,
    trackIndex: number,
    tracks: TrackInfo[],
    positionedIssues: PositionedIssue[],
  ): void {
    if (issuesOnTrack.length === 0) return;

    while (tracks.length <= trackIndex) {
      tracks.push({ intervals: [] });
    }

    const windowDurationMs = window.end - window.start;
    const totalIssueDurationOnTrack = this.getTotalDuration(issuesOnTrack);
    const totalFreeSpaceMs = windowDurationMs - totalIssueDurationOnTrack;
    const gapSize = totalFreeSpaceMs > 0 ? totalFreeSpaceMs / (issuesOnTrack.length + 1) : this.MINIMAL_GAP_MS;

    let currentTime = window.start + gapSize;
    for (const issue of issuesOnTrack) {
      const durationMs = this.getIssueDurationMsWithMinWidth(issue);
      this.commitPlacement(issue, currentTime, durationMs, trackIndex, tracks, positionedIssues);
      currentTime += durationMs + gapSize;
    }
  }

  private commitPlacement(
    issue: Issue,
    startTime: number,
    durationMs: number,
    trackIndex: number,
    tracks: TrackInfo[],
    finalLayout: PositionedIssue[],
  ): void {
    tracks[trackIndex].intervals.push({ start: startTime, end: startTime + durationMs });
    finalLayout.push({
      issue,
      track: trackIndex,
      style: this.calculateBarPosition(new Date(startTime), durationMs / (1000 * 3600 * 24)),
    });
  }

  private calculateProgress(): void {
    const total = this.milestone.openIssueCount + this.milestone.closedIssueCount;
    this.progressPercentage = total === 0 ? 0 : Math.round((this.milestone.closedIssueCount / total) * 100);
  }

  private isMilestoneInCurrentQuarter(): boolean {
    if (!this.milestone.dueOn) return false;

    const today = new Date();
    const currentQuarter = Math.floor(today.getMonth() / 3);
    const currentYear = today.getFullYear();

    const milestoneQuarter = Math.floor(this.milestone.dueOn.getMonth() / 3);
    const milestoneYear = this.milestone.dueOn.getFullYear();

    return currentYear === milestoneYear && currentQuarter === milestoneQuarter;
  }

  private getMilestoneQuarterWindow(): PlanningWindow {
    const due = new Date(this.milestone.dueOn!);
    const year = due.getFullYear();
    const quarterIndex = Math.floor(due.getMonth() / 3);
    const quarterStartDate = new Date(year, quarterIndex * 3, 1);
    const quarterEndDate = new Date(year, quarterIndex * 3 + 3, 0);
    quarterEndDate.setHours(23, 59, 59, 999);
    return { start: quarterStartDate.getTime(), end: quarterEndDate.getTime() };
  }

  private getProportionalWindowsForTrack(
    group1Issues: Issue[],
    group2Issues: Issue[],
    window: PlanningWindow,
  ): { window1: PlanningWindow; window2: PlanningWindow } {
    const duration1 = this.getTotalDuration(group1Issues);
    const duration2 = this.getTotalDuration(group2Issues);
    const totalDurationOnTrack = duration1 + duration2;
    const windowDuration = window.end - window.start;

    if (totalDurationOnTrack === 0 || windowDuration <= 0) {
      return { window1: window, window2: { start: window.end, end: window.end } };
    }

    const ratio1 = duration1 / totalDurationOnTrack;
    const window1End = window.start + windowDuration * ratio1;

    return {
      window1: { start: window.start, end: window1End },
      window2: { start: window1End, end: window.end },
    };
  }

  private getTotalDuration(issues: Issue[]): number {
    return issues.reduce((sum, issue) => sum + this.getIssueDurationMsWithMinWidth(issue), 0);
  }

  private estimateTrackCount(issues: Issue[], window: PlanningWindow): number {
    const totalDurationMs = this.getTotalDuration(issues);
    const windowDurationMs = window.end - window.start;
    const effectiveWindowMs = windowDurationMs * 0.98;
    return effectiveWindowMs > 0 ? Math.ceil(totalDurationMs / effectiveWindowMs) || 1 : 1;
  }

  private groupIssuesIntoTracks(issues: Issue[], trackCount: number): Issue[][] {
    const issuesByTrack: Issue[][] = Array.from({ length: trackCount }, () => []);
    for (const [index, issue] of issues.entries()) {
      issuesByTrack[index % trackCount].push(issue);
    }
    return issuesByTrack;
  }

  private getPlanningWindows(): { closedWindow: PlanningWindow; openWindow: PlanningWindow } | Record<string, never> {
    if (!this.milestone.dueOn) return {};

    const due = new Date(this.milestone.dueOn);
    const year = due.getFullYear();
    const quarterIndex = Math.floor(due.getMonth() / 3);
    const quarterStartDate = new Date(year, quarterIndex * 3, 1);
    const quarterEndDate = new Date(year, quarterIndex * 3 + 3, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const planningMidpoint = new Date(
      Math.max(quarterStartDate.getTime(), Math.min(today.getTime(), quarterEndDate.getTime())),
    );

    return {
      closedWindow: { start: quarterStartDate.getTime(), end: planningMidpoint.getTime() },
      openWindow: { start: planningMidpoint.getTime(), end: quarterEndDate.getTime() },
    };
  }

  private calculateBarPosition(startDate: Date, durationDays: number): Record<string, string> {
    const endDate = new Date(startDate.getTime() + durationDays * 24 * 60 * 60 * 1000);
    const timelineEndDate = new Date(this.timelineStartDate);
    timelineEndDate.setDate(timelineEndDate.getDate() + this.totalTimelineDays);

    if (endDate < this.timelineStartDate || startDate > timelineEndDate) {
      return { display: 'none' };
    }

    const clampedStartTime = Math.max(startDate.getTime(), this.timelineStartDate.getTime());
    const clampedEndTime = Math.min(endDate.getTime(), timelineEndDate.getTime());
    const startDays = (clampedStartTime - this.timelineStartDate.getTime()) / (1000 * 3600 * 24);
    const clampedDurationDays = (clampedEndTime - clampedStartTime) / (1000 * 3600 * 24);
    const widthPercentage = (clampedDurationDays / this.totalTimelineDays) * 100;

    return {
      left: `${(startDays / this.totalTimelineDays) * 100}%`,
      width: `${Math.max(widthPercentage, this.MIN_ISSUE_WIDTH_PERCENTAGE)}%`,
    };
  }

  private getIssueDurationMs(issue: Issue): number {
    return (issue.points ?? this.DEFAULT_POINTS) * 24 * 60 * 60 * 1000;
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
