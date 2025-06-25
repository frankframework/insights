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
  private readonly MIN_ISSUE_WIDTH_PERCENTAGE = 3;
  private readonly MINIMAL_GAP_MS = 60 * 60 * 1000;

  ngOnInit(): void {
    this.calculateProgress();
    if (this.milestone.dueOn) {
      this.runAdvancedLayoutAlgorithm();
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

  private runAdvancedLayoutAlgorithm(): void {
    if (!this.milestone.dueOn) return;

    // 1. SETUP: Definieer tijdvensters en sorteer issues
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

    const closedWindow = { start: quarterStartDate.getTime(), end: planningMidpoint.getTime() };
    const openWindow = { start: planningMidpoint.getTime(), end: quarterEndDate.getTime() };

    const closedIssues = this.sortIssuesByPriority(this.issues.filter((index) => index.state === GitHubStates.CLOSED));
    const openIssues = this.sortIssuesByPriority(this.issues.filter((index) => index.state === GitHubStates.OPEN));

    const finalPositionedIssues: PositionedIssue[] = [];
    let tracks: TrackInfo[] = [];

    // 2. PLAATS GESLOTEN ISSUES
    tracks = this.distributeIssuesInBlock(closedIssues, closedWindow, tracks, finalPositionedIssues);

    // 3. PLAATS OPEN ISSUES
    tracks = this.distributeIssuesInBlock(openIssues, openWindow, tracks, finalPositionedIssues);

    // 4. FINALIZEER
    this.positionedIssues = finalPositionedIssues;
    this.trackCount = Math.max(1, tracks.length);
  }

  /**
   * AANGEPAST: De berekening van benodigde tracks en de tussenruimte is verfijnd.
   */
  private distributeIssuesInBlock(
    issues: Issue[],
    window: { start: number; end: number },
    tracks: TrackInfo[],
    positionedIssues: PositionedIssue[],
  ): TrackInfo[] {
    if (issues.length === 0) {
      return tracks;
    }

    // Stap 1: Bereken het benodigde aantal tracks voor dit blok robuuster.
    const totalDurationMs = issues.reduce((sum, issue) => sum + this.getIssueDurationMsWithMinWidth(issue), 0);
    const windowDurationMs = window.end - window.start;
    // We gebruiken 98% van de tijd om een buffer te hebben voor imperfecte packing.
    const effectiveWindowMs = windowDurationMs * 0.98;
    const numberTracksForBlock = effectiveWindowMs > 0 ? Math.ceil(totalDurationMs / effectiveWindowMs) || 1 : 1;

    // Stap 2: Verdeel de issues over de tracks (in-memory) voordat we plaatsen.
    const issuesByTrack: Issue[][] = Array.from({ length: numberTracksForBlock }, () => []);
    for (const [index, issue] of issues.entries()) {
      const targetTrackIndex = index % numberTracksForBlock;
      issuesByTrack[targetTrackIndex].push(issue);
    }

    // Stap 3: Plaats de issues per track, met evenredige verdeling van ruimte.
    for (const [trackIndex, issuesOnThisTrack] of issuesByTrack.entries()) {
      if (issuesOnThisTrack.length === 0) continue;

      while (tracks.length <= trackIndex) {
        tracks.push({ intervals: [] });
      }

      // AANGEPAST: De spacing is nu dynamisch en niet meer afhankelijk van een grote, vaste buffer.
      const totalIssueDurationOnTrack = issuesOnThisTrack.reduce(
        (sum, issue) => sum + this.getIssueDurationMsWithMinWidth(issue),
        0,
      );
      const totalFreeSpaceMs = windowDurationMs - totalIssueDurationOnTrack;
      // Als er vrije ruimte is, verdeel die. Zo niet, gebruik een zeer kleine buffer.
      const gapSize = totalFreeSpaceMs > 0 ? totalFreeSpaceMs / (issuesOnThisTrack.length + 1) : this.MINIMAL_GAP_MS;

      let currentTime = window.start + gapSize;

      for (const issue of issuesOnThisTrack) {
        const durationMs = this.getIssueDurationMsWithMinWidth(issue);
        this.commitPlacement(issue, currentTime, durationMs, trackIndex, tracks, positionedIssues);
        currentTime += durationMs + gapSize;
      }
    }

    return tracks;
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

  private getIssueDurationMs(issue: Issue): number {
    const durationDays = issue.points ?? this.DEFAULT_POINTS;
    return durationDays * 24 * 60 * 60 * 1000;
  }

  private getIssueDurationMsWithMinWidth(issue: Issue): number {
    const durationMs = this.getIssueDurationMs(issue);
    const minDurationInMs = this.totalTimelineDays * (this.MIN_ISSUE_WIDTH_PERCENTAGE / 100) * 24 * 60 * 60 * 1000;
    return Math.max(durationMs, minDurationInMs);
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

  private sortIssuesByPriority(issues: Issue[]): Issue[] {
    const priorityOrder: Record<string, number> = { critical: 1, high: 2, medium: 3, low: 4, no: 5 };
    return [...issues].sort((a, b) => {
      const priorityA = priorityOrder[this.getPriorityKey(a.issuePriority?.name)] ?? 5;
      const priorityB = priorityOrder[this.getPriorityKey(b.issuePriority?.name)] ?? 5;
      if (priorityA !== priorityB) {
        return priorityA - priorityB;
      }
      const pointsA = a.points ?? this.DEFAULT_POINTS;
      const pointsB = b.points ?? this.DEFAULT_POINTS;
      if (pointsA !== pointsB) {
        return pointsB - pointsA;
      }
      return b.number - a.number;
    });
  }

  private getPriorityKey(priorityName: string | undefined): string {
    if (!priorityName) return 'no';
    const lowerCaseName = priorityName.toLowerCase();
    const keys = ['critical', 'high', 'medium', 'low'];
    for (const key of keys) {
      if (lowerCaseName.includes(key)) return key;
    }
    return 'no';
  }
}
