import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';

interface PositionedIssue {
  issue: Issue;
  style: Record<string, string>;
  track: number;
}

interface IssuePlan {
  issue: Issue;
  startDate: Date;
  endDate: Date;
  start: number;
  end: number;
}

interface TrackInfo {
  intervals: { start: number; end: number }[];
}

@Component({
  selector: 'app-milestone-row',
  standalone: true,
  imports: [CommonModule, IssueBarComponent],
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
  private readonly GAP_BUFFER_MS = 12 * 60 * 60 * 1000; // 12 uur

  ngOnInit(): void {
    this.calculateProgress();
    if (this.milestone.dueOn) {
      this.planAndLayoutIssues();
    }
  }

  public getIssuesForTrack(trackNumber: number): PositionedIssue[] {
    return this.positionedIssues.filter((p) => p.track === trackNumber);
  }

  public getTracks(): number[] {
    return Array.from({ length: this.trackCount }, (_, index) => index);
  }

  private calculateProgress(): void {
    const total = this.milestone.openIssueCount + this.milestone.closedIssueCount;
    this.progressPercentage = total === 0 ? 0 : Math.round((this.milestone.closedIssueCount / total) * 100);
  }

  private planAndLayoutIssues(): void {
    if (!this.milestone.dueOn) return;

    const due = new Date(this.milestone.dueOn);
    const year = due.getFullYear();
    const quarterIndex = Math.floor(due.getMonth() / 3);
    const quarterStartDate = new Date(year, quarterIndex * 3, 1);
    const quarterEndDate = new Date(year, quarterIndex * 3 + 3, 0);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const openIssues = this.sortIssuesByPriority(this.issues.filter((index) => index.state === GitHubStates.OPEN));
    const closedIssues = this.sortIssuesByPriority(this.issues.filter((index) => index.state === GitHubStates.CLOSED));

    const finalLayout: PositionedIssue[] = [];
    const tracks: TrackInfo[] = [];

    const closedIssuesTimeWindowMs = Math.max(0, today.getTime() - quarterStartDate.getTime());
    const totalClosedIssuesDurationMs = closedIssues.reduce(
      (sum, issue) => sum + (issue.points ?? this.DEFAULT_POINTS) * 24 * 60 * 60 * 1000,
      0,
    );
    const closedFreeSpaceMs = Math.max(0, closedIssuesTimeWindowMs - totalClosedIssuesDurationMs);
    const closedGapMs = closedIssues.length > 0 ? closedFreeSpaceMs / closedIssues.length : 0;
    let pastDateCursor = new Date(quarterStartDate.getTime() + closedGapMs / 2);

    for (const issue of closedIssues) {
      const durationDays = issue.points ?? this.DEFAULT_POINTS;
      const startDate = new Date(pastDateCursor);
      const endDate = new Date(startDate);
      endDate.setDate(endDate.getDate() + durationDays);

      if (endDate > today) endDate.setTime(today.getTime());
      if (startDate > endDate) startDate.setTime(endDate.getTime());

      this.placeIssueOnTrack(
        { issue, startDate, endDate, start: startDate.getTime(), end: endDate.getTime() },
        tracks,
        finalLayout,
      );
      pastDateCursor.setTime(endDate.getTime() + closedGapMs);
    }

    // 4. Plan de openstaande "to-do" issues
    // Spreid de issues terug vanaf het einde van het kwartaal tot vandaag.
    const openIssuesTimeWindowMs = Math.max(0, quarterEndDate.getTime() - today.getTime());
    const totalOpenIssuesDurationMs = openIssues.reduce(
      (sum, issue) => sum + (issue.points ?? this.DEFAULT_POINTS) * 24 * 60 * 60 * 1000,
      0,
    );
    const openFreeSpaceMs = Math.max(0, openIssuesTimeWindowMs - totalOpenIssuesDurationMs);
    const openGapMs = openIssues.length > 0 ? openFreeSpaceMs / openIssues.length : 0;
    let futureDateCursor = new Date(quarterEndDate.getTime() - openGapMs / 2);

    for (const issue of [...openIssues].reverse()) {
      // Laagste prio eerst, zodat ze het verst in de toekomst komen
      const durationDays = issue.points ?? this.DEFAULT_POINTS;
      const endDate = new Date(futureDateCursor);
      const startDate = new Date(endDate);
      startDate.setDate(startDate.getDate() - durationDays);

      if (startDate < today) startDate.setTime(today.getTime());
      if (endDate < startDate) endDate.setTime(startDate.getTime());

      this.placeIssueOnTrack(
        { issue, startDate, endDate, start: startDate.getTime(), end: endDate.getTime() },
        tracks,
        finalLayout,
      );
      futureDateCursor.setTime(startDate.getTime() - openGapMs);
    }

    this.trackCount = Math.max(1, tracks.length);
    this.positionedIssues = finalLayout;
  }

  private placeIssueOnTrack(plan: IssuePlan, tracks: TrackInfo[], finalLayout: PositionedIssue[]): void {
    const minDurationInDays = this.totalTimelineDays * (this.MIN_ISSUE_WIDTH_PERCENTAGE / 100);
    const minDurationInMs = minDurationInDays * 24 * 60 * 60 * 1000;

    if (plan.end <= plan.start) {
      plan.end = plan.start + minDurationInMs;
    }

    const actualDurationInMs = plan.end - plan.start;
    const effectiveDurationInMs = Math.max(actualDurationInMs, minDurationInMs);

    const visualInterval = {
      start: plan.start,
      end: plan.start + effectiveDurationInMs + this.GAP_BUFFER_MS,
    };

    let bestTrackIndex = -1;

    for (const [index, track] of tracks.entries()) {
      const hasOverlap = track.intervals.some(
        (existing) => visualInterval.start < existing.end && visualInterval.end > existing.start,
      );
      if (!hasOverlap) {
        bestTrackIndex = index;
        break;
      }
    }

    if (bestTrackIndex === -1) {
      tracks.push({ intervals: [] });
      bestTrackIndex = tracks.length - 1;
    }

    tracks[bestTrackIndex].intervals.push(visualInterval);
    finalLayout.push({
      issue: plan.issue,
      track: bestTrackIndex,
      style: this.calculateBarPosition(plan.startDate, plan.issue.points ?? this.DEFAULT_POINTS),
    });
  }

  private calculateBarPosition(startDate: Date, durationDays: number): Record<string, string> {
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + durationDays);
    const timelineEndDate = new Date(this.timelineStartDate);
    timelineEndDate.setDate(timelineEndDate.getDate() + this.totalTimelineDays);

    if (endDate < this.timelineStartDate || startDate > timelineEndDate) return { display: 'none' };

    const clampedStartTime = Math.max(startDate.getTime(), this.timelineStartDate.getTime());
    const clampedEndTime = Math.min(endDate.getTime(), timelineEndDate.getTime());

    const startDays = (clampedStartTime - this.timelineStartDate.getTime()) / (1000 * 3600 * 24);
    const durationInDays = (clampedEndTime - clampedStartTime) / (1000 * 3600 * 24);

    const widthPercentage = (durationInDays / this.totalTimelineDays) * 100;

    return {
      left: `${(startDays / this.totalTimelineDays) * 100}%`,
      width: `${Math.max(widthPercentage, this.MIN_ISSUE_WIDTH_PERCENTAGE)}%`,
    };
  }

  private sortIssuesByPriority(issues: Issue[]): Issue[] {
    const priorityOrder: Record<string, number> = { critical: 1, high: 2, medium: 3, low: 4, no: 5 };
    return [...issues].sort((a, b) => {
      const priorityA = priorityOrder[a.issuePriority?.name.toLowerCase() ?? 'no'] ?? 5;
      const priorityB = priorityOrder[b.issuePriority?.name.toLowerCase() ?? 'no'] ?? 5;
      if (priorityA !== priorityB) return priorityA - priorityB;
      return b.number - a.number;
    });
  }
}
