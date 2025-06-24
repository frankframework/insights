import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IssueBarComponent } from '../issue-bar/issue-bar.component';
import { Milestone } from '../../../services/milestone.service';
import { Issue } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';

export interface PositionedIssue {
  issue: Issue;
  style: Record<string, string>;
  track: number;
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
    const closedIssues = this.sortIssuesByPriority(this.issues.filter((issue) => issue.state === GitHubStates.CLOSED));
    const openIssues = this.sortIssuesByPriority(this.issues.filter((issue) => issue.state === GitHubStates.OPEN));
    const allSortedIssues = [...closedIssues, ...openIssues];

    if (!this.milestone.dueOn) return;

    const totalDuration = allSortedIssues.reduce(
      (accumulator, issue) => accumulator + (issue.points ?? this.DEFAULT_POINTS),
      0,
    );
    const workBlockStartDate = new Date(this.milestone.dueOn);
    workBlockStartDate.setDate(workBlockStartDate.getDate() - totalDuration);

    const issuePlans: { issue: Issue; startDate: Date; endDate: Date }[] = [];
    let currentPlanningDate = new Date(workBlockStartDate);

    for (const issue of allSortedIssues) {
      const duration = issue.points ?? this.DEFAULT_POINTS;
      const issueStartDate = new Date(currentPlanningDate);
      const issueEndDate = new Date(issueStartDate);
      issueEndDate.setDate(issueStartDate.getDate() + duration);
      issuePlans.push({ issue, startDate: issueStartDate, endDate: issueEndDate });
      currentPlanningDate = issueEndDate;
    }

    const finalLayout: PositionedIssue[] = [];
    const trackEndTimes: number[] = [];

    for (const plan of issuePlans) {
      let placed = false;
      for (let index = 0; index < trackEndTimes.length; index++) {
        if (plan.startDate.getTime() >= trackEndTimes[index]!) {
          trackEndTimes[index] = plan.endDate.getTime();
          finalLayout.push({
            issue: plan.issue,
            track: index,
            style: this.calculateBarPosition(plan.startDate, plan.issue.points ?? this.DEFAULT_POINTS),
          });
          placed = true;
          break;
        }
      }

      if (!placed) {
        trackEndTimes.push(plan.endDate.getTime());
        finalLayout.push({
          issue: plan.issue,
          track: trackEndTimes.length - 1,
          style: this.calculateBarPosition(plan.startDate, plan.issue.points ?? this.DEFAULT_POINTS),
        });
      }
    }

    this.trackCount = Math.max(1, trackEndTimes.length);
    this.positionedIssues = finalLayout;
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
    const duration = Math.max(1, (clampedEndTime - clampedStartTime) / (1000 * 3600 * 24));

    return {
      left: `${(startDays / this.totalTimelineDays) * 100}%`,
      width: `${(duration / this.totalTimelineDays) * 100}%`,
    };
  }

  private sortIssuesByPriority(issues: Issue[]): Issue[] {
    const priorityOrder: Record<string, number> = { critical: 1, high: 2, medium: 3, low: 4, no: 5 };
    return [...issues].sort((a, b) => {
      const priorityAKey = a.issuePriority?.name.toLowerCase() ?? 'no';
      const priorityBKey = b.issuePriority?.name.toLowerCase() ?? 'no';
      const orderA = priorityOrder[priorityAKey] ?? 6;
      const orderB = priorityOrder[priorityBKey] ?? 6;
      return orderA - orderB;
    });
  }
}
