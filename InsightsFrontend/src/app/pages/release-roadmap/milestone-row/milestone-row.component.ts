import { Component, Input, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue, IssuePriority } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';

interface PositionedIssue {
  issue: Issue;
  style: Record<string, string>;
  track: number;
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
export class MilestoneRowComponent implements OnInit {
  @Input({ required: true }) milestone!: Milestone;
  @Input({ required: true }) issues: Issue[] = [];
  @Input({ required: true }) timelineStartDate!: Date;
  @Input({ required: true }) timelineEndDate!: Date;
  @Input({ required: true }) totalTimelineDays!: number;
  @Input({ required: true }) quarters!: { name: string; monthCount: number }[];
  @Input() isLast = false;

  public positionedIssues: PositionedIssue[] = [];
  public trackCount = 1;
  public progressPercentage = 0;

  private readonly DEFAULT_POINTS = 3;
  private readonly MIN_ISSUE_WIDTH_PERCENTAGE = 3;
  private readonly GAP_MS = 24 * 3600 * 1000;

  ngOnInit(): void {
    this.calculateProgress();
    this.runLayoutAlgorithm();
  }

  public getIssuesForTrack(trackNumber: number): PositionedIssue[] {
    return this.positionedIssues.filter((p) => p.track === trackNumber);
  }

  public getTracks(): number[] {
    return Array.from({ length: this.trackCount }, (_, index) => index);
  }

  private runLayoutAlgorithm(): void {
    if (this.issues.length === 0) return;

    this.positionedIssues = [];
    let overallMaxTrackCount = 0;

    const issuesByQuarter = this.distributeIssuesIntoQuarters();

    for (const [quarterKey, quarterIssues] of issuesByQuarter.entries()) {
      const quarterWindow = this.getWindowForQuarter(quarterKey);
      if (!quarterWindow) continue;

      const { closedWindow, openWindow } = this.getPlanningWindowsForQuarter(quarterWindow);

      const closedLayout = this.layoutIssuesWithEvenSpacing(quarterIssues.closed, closedWindow);
      const openLayout = this.layoutIssuesWithEvenSpacing(quarterIssues.open, openWindow);

      this.positionedIssues.push(...closedLayout.positionedIssues, ...openLayout.positionedIssues);

      const maxTracksInQuarter = Math.max(closedLayout.trackCount, openLayout.trackCount);
      if (maxTracksInQuarter > overallMaxTrackCount) {
        overallMaxTrackCount = maxTracksInQuarter;
      }
    }

    this.trackCount = Math.max(1, overallMaxTrackCount);
  }

  private distributeIssuesIntoQuarters(): QuarterIssueMap {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const currentQuarterStart = this.getQuarterFromDate(today);
    const quarterMap: QuarterIssueMap = new Map();

    // Initialize map for all visible quarters to ensure they exist
    for (const q of this.quarters) {
      quarterMap.set(q.name, { open: [], closed: [] });
    }

    for (const issue of this.issues) {
      const sortedIssue = this.getSortedIssues(
        issue.state === GitHubStates.OPEN ? [issue] : [],
        issue.state === GitHubStates.CLOSED ? [issue] : [],
      );
      if (!sortedIssue) continue;

      if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
        // Exception 1: Closed issues are always placed in the quarter they were closed in.
        const closedQuarter = this.getQuarterFromDate(new Date(issue.closedAt));
        const quarterKey = this.getQuarterKey(closedQuarter);
        if (!quarterMap.has(quarterKey)) quarterMap.set(quarterKey, { open: [], closed: [] });
        quarterMap.get(quarterKey)!.closed.push(issue);
      } else if (issue.state === GitHubStates.OPEN) {
        const milestoneDueQuarter = this.milestone.dueOn
          ? this.getQuarterFromDate(new Date(this.milestone.dueOn))
          : currentQuarterStart;

        if (milestoneDueQuarter.getTime() < currentQuarterStart.getTime()) {
          // Exception 2: Open issues from past milestones are moved to the current quarter.
          const currentQuarterKey = this.getQuarterKey(currentQuarterStart);
          if (quarterMap.has(currentQuarterKey)) {
            quarterMap.get(currentQuarterKey)!.open.push(issue);
          }
        } else {
          // Standard case: Open issues are placed in their milestone's scheduled quarter.
          const quarterKey = this.getQuarterKey(milestoneDueQuarter);
          if (!quarterMap.has(quarterKey)) quarterMap.set(quarterKey, { open: [], closed: [] });
          quarterMap.get(quarterKey)!.open.push(issue);
        }
      }
    }

    // Sort issues within each bucket by priority
    for (const [_, issues] of quarterMap) {
      issues.open = this.getSortedIssues(issues.open, []);
      issues.closed = this.getSortedIssues([], issues.closed);
    }

    return quarterMap;
  }

  private getWindowForQuarter(quarterKey: string): PlanningWindow | null {
    const match = quarterKey.match(/Q(\d) (\d{4})/);
    if (!match) return null;

    const quarterNumber = Number.parseInt(match[1], 10);
    const year = Number.parseInt(match[2], 10);

    const startDate = new Date(year, (quarterNumber - 1) * 3, 1);
    const endDate = new Date(year, quarterNumber * 3, 0);
    endDate.setHours(23, 59, 59, 999);

    return { start: startDate.getTime(), end: endDate.getTime() };
  }

  private getPlanningWindowsForQuarter(quarterWindow: PlanningWindow): {
    closedWindow: PlanningWindow;
    openWindow: PlanningWindow;
  } {
    const today = new Date();
    today.setHours(23, 59, 59, 999); // Use end of today as the split point
    const todayMs = today.getTime();

    const start = quarterWindow.start;
    const end = quarterWindow.end;

    if (todayMs < start) {
      // Quarter is entirely in the future
      return { closedWindow: { start, end: start }, openWindow: { start, end } };
    }
    if (todayMs > end) {
      // Quarter is entirely in the past
      return { closedWindow: { start, end }, openWindow: { start: end, end } };
    }
    // Quarter contains today
    return {
      closedWindow: { start, end: todayMs },
      openWindow: { start: todayMs, end },
    };
  }

  private layoutIssuesWithEvenSpacing(issues: Issue[], window: PlanningWindow): LayoutContext {
    if (issues.length === 0 || window.start >= window.end) {
      return { positionedIssues: [], trackCount: 0 };
    }

    const estimatedTracks = this.estimateTrackCount(issues, window);
    const issuesByTrack = this.distributeIssuesRoundRobin(issues, estimatedTracks);

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
        const startTime = cursor;
        positionedIssues.push(this.createPositionedIssue(issue, startTime, durationMs, trackIndex));
        cursor += durationMs + gapSize;
      }
    }
    return { positionedIssues, trackCount: issuesByTrack.size };
  }

  // --- Helper functions (mostly unchanged, but some are new or adapted) ---

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

  private estimateTrackCount(issues: Issue[], window: PlanningWindow): number {
    const windowDurationMs = window.end - window.start;
    if (windowDurationMs <= 0) return issues.length;

    const totalDurationWithGaps = issues.reduce((sum, issue) => {
      return sum + this.getIssueDurationMsWithMinWidth(issue) + this.GAP_MS;
    }, -this.GAP_MS); // No gap needed at the start

    return Math.max(1, Math.ceil(totalDurationWithGaps / windowDurationMs));
  }

  private distributeIssuesRoundRobin(issues: Issue[], trackCount: number): Map<number, Issue[]> {
    const issuesByTrack = new Map<number, Issue[]>();
    if (trackCount === 0) return issuesByTrack;

    for (let index = 0; index < trackCount; index++) {
      issuesByTrack.set(index, []);
    }
    for (const [index, issue] of issues.entries()) {
      issuesByTrack.get(index % trackCount)!.push(issue);
    }
    return issuesByTrack;
  }

  private getSortedIssues(openIssues: Issue[], closedIssues: Issue[]): Issue[] {
    const priorityOrder: Record<string, number> = { critical: 1, high: 2, medium: 3, low: 4, no: 5 };
    const sorter = (a: Issue, b: Issue) => {
      const priorityA = priorityOrder[this.getPriorityKey(a.issuePriority)] ?? 5;
      const priorityB = priorityOrder[this.getPriorityKey(b.issuePriority)] ?? 5;
      if (priorityA !== priorityB) return priorityA - priorityB;

      const pointsA = a.points ?? this.DEFAULT_POINTS;
      const pointsB = b.points ?? this.DEFAULT_POINTS;
      if (pointsA !== pointsB) return pointsB - pointsA; // Higher points first

      return b.number - a.number; // Fallback to issue number
    };

    const sortedOpen = [...openIssues].sort(sorter);
    const sortedClosed = [...closedIssues].sort(sorter);

    return [...sortedClosed, ...sortedOpen];
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
    const durationMs = points * 24 * 60 * 60 * 1000; // 1 point = 1 day
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

  private calculateProgress(): void {
    const total = this.milestone.openIssueCount + this.milestone.closedIssueCount;
    this.progressPercentage = total === 0 ? 0 : Math.round((this.milestone.closedIssueCount / total) * 100);
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
