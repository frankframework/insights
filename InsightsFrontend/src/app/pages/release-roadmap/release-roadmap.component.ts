import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, forkJoin, of, switchMap, tap, finalize, map } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { RoadmapToolbarComponent } from './roadmap-toolbar/roadmap-toolbar.component';
import { TimelineHeaderComponent } from './timeline-header/timeline-header.component';
import { MilestoneRowComponent } from './milestone-row/milestone-row.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { Issue, IssueService } from '../../services/issue.service';
import { Milestone, MilestoneService } from '../../services/milestone.service';

interface Version {
  major: number;
  minor: number;
  patch: number;
  source: string;
}

@Component({
  selector: 'app-release-roadmap',
  standalone: true,
  imports: [CommonModule, RoadmapToolbarComponent, TimelineHeaderComponent, MilestoneRowComponent, LoaderComponent],
  templateUrl: './release-roadmap.component.html',
  styleUrls: ['./release-roadmap.component.scss'],
})
export class ReleaseRoadmapComponent implements OnInit, AfterViewInit {
  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLDivElement>;

  public isLoading = true;
  public openMilestones: Milestone[] = [];
  public milestoneIssues = new Map<string, Issue[]>();
  public timelineStartDate!: Date;
  public timelineEndDate!: Date;
  public months: Date[] = [];
  public quarters: { name: string; monthCount: number }[] = [];
  public totalDays = 0;
  public displayDate!: Date;
  protected todayOffsetPercentage = 0;
  private viewInitialized = false;

  constructor(
    private milestoneService: MilestoneService,
    private issueService: IssueService,
    private toastrService: ToastrService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.resetPeriod();
  }

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.scrollToToday();
  }

  public changePeriod(months: number): void {
    this.displayDate.setMonth(this.displayDate.getMonth() + months);
    this.displayDate = new Date(this.displayDate);
    this.generateTimelineFromPeriod();
  }

  public resetPeriod(): void {
    this.displayDate = new Date();
    this.generateTimelineFromPeriod();
  }

  public getIssuesForMilestone(milestoneId: string): Issue[] {
    return this.milestoneIssues.get(milestoneId) || [];
  }

  private generateTimelineFromPeriod(): void {
    if (!this.displayDate) return;
    const startMonth = this.displayDate.getMonth() < 6 ? 0 : 6;
    const year = this.displayDate.getFullYear();
    const startDate = new Date(year, startMonth, 1);
    const endDate = new Date(year, startMonth + 6, 0); // Laatste dag van de periode

    this.timelineStartDate = startDate;
    this.timelineEndDate = endDate;

    this.months = [];
    let currentDate = new Date(startDate);
    while (currentDate.getMonth() !== endDate.getMonth() || currentDate.getFullYear() !== endDate.getFullYear()) {
      this.months.push(new Date(currentDate));
      currentDate.setMonth(currentDate.getMonth() + 1);
    }
    this.months.push(new Date(currentDate)); // Voeg de laatste maand toe

    this.generateQuarters();
    this.totalDays = (endDate.getTime() - startDate.getTime()) / (1000 * 3600 * 24);
    const today = new Date();
    const daysFromStart = (today.getTime() - startDate.getTime()) / (1000 * 3600 * 24);
    this.todayOffsetPercentage = (daysFromStart / this.totalDays) * 100;

    this.loadRoadmapData();
  }

  private loadRoadmapData(): void {
    this.isLoading = true;
    this.milestoneService
      .getOpenMilestones()
      .pipe(
        map((apiMilestones) => this.parseMilestones(apiMilestones)),
        switchMap((parsedMilestones) => {
          if (parsedMilestones.length === 0) {
            this.openMilestones = [];
            return of({ milestones: [] });
          }
          const issueRequests = parsedMilestones.map((m) =>
            this.issueService.getIssuesByMilestoneId(m.id).pipe(
              map((issues) => ({ milestoneId: m.id, issues })),
              catchError(() => of({ milestoneId: m.id, issues: [] })),
            ),
          );
          return forkJoin(issueRequests).pipe(
            map((issueResults) => {
              this.milestoneIssues.clear();
              for (const result of issueResults) this.milestoneIssues.set(result.milestoneId, result.issues);
              return { milestones: parsedMilestones };
            }),
          );
        }),
        // AANGEPAST: Nieuwe planningslogica wordt hier toegepast.
        map(({ milestones }) => this.scheduleMilestones(milestones)),
        map((finalMilestones) => this.sortMilestones(finalMilestones)),
        map((sortedMilestones) => this.filterMilestonesInView(sortedMilestones)),
        tap((finalMilestones) => (this.openMilestones = finalMilestones)),
        catchError((error) => {
          this.toastrService.error('Kon roadmap data niet laden.', 'Fout');
          console.error(error);
          return of(null);
        }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
          this.scrollToToday();
        }),
      )
      .subscribe();
  }

  /**
   * NIEUW: parseert een versiestring naar een object.
   */
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

  /**
   * NIEUW: De kern van de nieuwe planningslogica.
   * Herverdeelt milestones over kwartalen op basis van versienummers.
   */
  private scheduleMilestones(milestones: Milestone[]): Milestone[] {
    const sortedByVersion = milestones
      .map((m) => ({ milestone: m, version: this.parseVersion(m.title) }))
      .filter((item) => item.version !== null)
      .sort((a, b) => {
        const vA = a.version!;
        const vB = b.version!;
        if (vA.major !== vB.major) return vA.major - vB.major;
        if (vA.minor !== vB.minor) return vA.minor - vB.minor;
        return vA.patch - vB.patch;
      });

    const scheduledMilestones: Milestone[] = [];
    const planningStartDate = new Date(); // Start planning vanaf vandaag
    let quarterCursor = new Date(planningStartDate);

    const isMajorRelease = (v: Version) => v.patch === 0;

    const majors = sortedByVersion.filter((m) => isMajorRelease(m.version!));
    const minors = sortedByVersion.filter((m) => !isMajorRelease(m.version!));

    // Plan eerst de majors, 1 per kwartaal
    for (const item of majors) {
      const year = quarterCursor.getFullYear();
      const quarterIndex = Math.floor(quarterCursor.getMonth() / 3);
      // Zet due date op het einde van het kwartaal
      const dueDate = new Date(year, quarterIndex * 3 + 3, 0);

      item.milestone.dueOn = dueDate;
      scheduledMilestones.push(item.milestone);

      // Schuif cursor naar volgend kwartaal
      quarterCursor.setMonth(quarterCursor.getMonth() + 3);
    }

    // Reset cursor om minors te plannen
    quarterCursor = new Date(planningStartDate);
    let lastMinorSeries = '';

    // Plan vervolgens de minors
    for (const item of minors) {
      const series = `${item.version!.major}.${item.version!.minor}`;
      if (series !== lastMinorSeries) {
        // Nieuwe minor serie, reset de kwartaalcursor naar de start
        quarterCursor = new Date(planningStartDate);
      }
      lastMinorSeries = series;

      const year = quarterCursor.getFullYear();
      const quarterIndex = Math.floor(quarterCursor.getMonth() / 3);
      const dueDate = new Date(year, quarterIndex * 3 + 3, 0);

      item.milestone.dueOn = dueDate;
      scheduledMilestones.push(item.milestone);

      quarterCursor.setMonth(quarterCursor.getMonth() + 3);
    }

    return scheduledMilestones;
  }

  private filterMilestonesInView(milestones: Milestone[]): Milestone[] {
    if (!this.timelineStartDate || !this.timelineEndDate) return milestones;
    const viewStartTime = this.timelineStartDate.getTime();
    const viewEndTime = this.timelineEndDate.getTime();

    return milestones.filter((milestone) => {
      if (!milestone.dueOn) return false;
      const dueTime = milestone.dueOn.getTime();
      // Toon milestone als de due date binnen de weergave valt.
      return dueTime >= viewStartTime && dueTime <= viewEndTime;
    });
  }

  private scrollToToday(): void {
    if (this.viewInitialized && this.scrollContainer?.nativeElement && this.todayOffsetPercentage > 0) {
      const container = this.scrollContainer.nativeElement;
      container.scrollLeft = (this.todayOffsetPercentage / 100) * container.scrollWidth - container.clientWidth / 3;
    }
  }

  private parseMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.map((m) => ({ ...m, dueOn: m.dueOn ? new Date(m.dueOn) : null }));
  }

  private sortMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.sort((a, b) => (a.dueOn?.getTime() ?? 0) - (b.dueOn?.getTime() ?? 0));
  }

  private generateQuarters(): void {
    this.quarters = [];
    if (this.months.length === 0) return;
    const startQuarter = Math.floor(this.timelineStartDate.getMonth() / 3);
    this.quarters.push(
      { name: `Q${startQuarter + 1}`, monthCount: 3 },
      { name: `Q${startQuarter + 2}`, monthCount: 3 },
    );
  }
}
