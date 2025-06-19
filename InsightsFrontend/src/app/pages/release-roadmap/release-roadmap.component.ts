import { Component, OnInit } from '@angular/core';
import { Milestone, MilestoneService } from '../../services/milestone.service';
import { Issue, IssueService } from '../../services/issue.service';
import { catchError, forkJoin, of, switchMap, tap } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-release-roadmap',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './release-roadmap.component.html',
  styleUrl: './release-roadmap.component.scss',
})
export class ReleaseRoadmapComponent implements OnInit {
  public isLoading = true;
  public openMilestones: Milestone[] = [];
  public milestoneIssues = new Map<Milestone, Issue[]>();

  constructor(
    private milestoneService: MilestoneService,
    private issueService: IssueService,
    private toastrService: ToastrService,
  ) {}

  ngOnInit(): void {
    this.loadRoadmapData();
  }

  private loadRoadmapData(): void {
    this.isLoading = true;
    this.milestoneService
      .getOpenMilestones()
      .pipe(
        switchMap((milestones) => {
          if (milestones.length === 0) {
            this.toastrService.info('No open milestones found.');
            this.openMilestones = [];
            return of([]);
          }

          this.openMilestones = milestones;

          const issueRequests = milestones.map((milestone) =>
            this.issueService.getIssuesByMilestoneId(milestone.id).pipe(
              catchError((error) => {
                console.error(`Failed to load issues for milestone ${milestone.title}:`, error);
                this.toastrService.error(`Failed to load issues for milestone ${milestone.title}.`);
                return of([]);
              }),
            ),
          );

          return forkJoin(issueRequests);
        }),
        tap((issuesByMilestone) => {
          for (const [index, milestone] of this.openMilestones.entries()) {
            this.milestoneIssues.set(milestone, issuesByMilestone[index]);
          }
        }),
        catchError((error) => {
          console.error('Failed to load open milestones:', error);
          this.toastrService.error('Failed to load open milestones. Please try again later.');
          this.openMilestones = [];
          this.milestoneIssues.clear();
          return of(null);
        }),
      )
      .subscribe({
        complete: () => {
          this.isLoading = false;
        },
      });
  }
}
