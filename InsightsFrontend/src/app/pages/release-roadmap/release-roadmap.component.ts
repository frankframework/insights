import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { catchError, finalize, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { RoadmapToolbarComponent } from './roadmap-toolbar/roadmap-toolbar.component';
import { TimelineHeaderComponent } from './timeline-header/timeline-header.component';
import { MilestoneRowComponent } from './milestone-row/milestone-row.component';
import { LoaderComponent } from '../../components/loader/loader.component';
import { Issue, IssueService } from '../../services/issue.service';
import { Milestone, MilestoneService } from '../../services/milestone.service';
import { GitHubStates } from '../../app.service';
import { IssueBarComponent } from './issue-bar/issue-bar.component';

interface Version {
  major: number;
  minor: number;
  patch: number;
  source: string;
}

@Component({
  selector: 'app-release-roadmap',
  standalone: true,
  imports: [
    CommonModule,
    RoadmapToolbarComponent,
    TimelineHeaderComponent,
    MilestoneRowComponent,
    LoaderComponent,
    IssueBarComponent,
  ],
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
  public currentPeriodLabel = '';
  protected todayOffsetPercentage = 0;
  private viewInitialized = false;
  private scheduledMilestones = new Map<string, Date>(); // Cache for scheduled due dates
  public issuesPerQuarter = new Map<string, { milestone: Milestone, issues: Issue[] }[]>();

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
    // Use a small timeout to ensure the DOM has been fully painted after data load
    setTimeout(() => this.scrollToToday(), 0);
  }

  public changePeriod(months: number): void {
    this.displayDate.setMonth(this.displayDate.getMonth() + months);
    this.displayDate = new Date(this.displayDate); // Force change detection
    this.generateTimelineFromPeriod();
  }

  public resetPeriod(): void {
    const today = new Date();
    const currentQuarterStartMonth = Math.floor(today.getMonth() / 3) * 3;
    this.displayDate = new Date(today.getFullYear(), currentQuarterStartMonth, 1);
    this.generateTimelineFromPeriod();
  }

  public clearScheduledDates(): void {
    this.scheduledMilestones.clear();
    this.loadRoadmapData();
  }

  public getIssuesForMilestone(milestoneId: string): Issue[] {
    return this.milestoneIssues.get(milestoneId) || [];
  }

  public getIssuesForMilestoneAndQuarter(milestone: Milestone, quarterName: string): Issue[] {
    const entries = this.issuesPerQuarter.get(quarterName) ?? [];
    const entry = entries.find(e => e.milestone.id === milestone.id);
    return entry ? entry.issues : [];
  }

  private generateTimelineFromPeriod(): void {
    if (!this.displayDate) return;

    this.calculateTimelineBoundaries();
    this.generateMonths();
    this.generateQuarters();
    this.calculateTodayMarkerPosition();
    this.loadRoadmapData();
  }

  private calculateTimelineBoundaries(): void {
    this.timelineStartDate = new Date(this.displayDate);
    const endDate = new Date(this.timelineStartDate);
    endDate.setMonth(endDate.getMonth() + 6);
    endDate.setDate(0);
    this.timelineEndDate = endDate;
  }

  private generateMonths(): void {
    this.months = [];
    let currentDate = new Date(this.timelineStartDate);
    while (currentDate <= this.timelineEndDate) {
      this.months.push(new Date(currentDate));
      currentDate.setMonth(currentDate.getMonth() + 1);
    }
  }

  private calculateTodayMarkerPosition(): void {
    this.totalDays = (this.timelineEndDate.getTime() - this.timelineStartDate.getTime()) / (1000 * 3600 * 24) + 1;
    const today = new Date();
    if (today < this.timelineStartDate || today > this.timelineEndDate) {
      this.todayOffsetPercentage = 0;
      return;
    }

    const totalDuration = this.timelineEndDate.getTime() - this.timelineStartDate.getTime();
    if (totalDuration <= 0) {
      this.todayOffsetPercentage = 0;
      return;
    }
    const todayOffset = today.getTime() - this.timelineStartDate.getTime();
    this.todayOffsetPercentage = (todayOffset / totalDuration) * 100;
  }

  private generateQuarters(): void {
    this.quarters = [];
    if (this.months.length === 0) return;

    const startYear = this.timelineStartDate.getFullYear();
    const startQuarterNumber = Math.floor(this.timelineStartDate.getMonth() / 3) + 1;
    const firstQuarterName = `Q${startQuarterNumber} ${startYear}`;
    this.quarters.push({ name: firstQuarterName, monthCount: 3 });

    const nextQuarterStartDate = new Date(this.timelineStartDate);
    nextQuarterStartDate.setMonth(nextQuarterStartDate.getMonth() + 3);
    const endYear = nextQuarterStartDate.getFullYear();
    const endQuarterNumber = Math.floor(nextQuarterStartDate.getMonth() / 3) + 1;
    const secondQuarterName = `Q${endQuarterNumber} ${endYear}`;
    this.quarters.push({ name: secondQuarterName, monthCount: 3 });

    this.currentPeriodLabel = `${firstQuarterName} - ${secondQuarterName}`;
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
              this.buildIssuesPerQuarterMap();
              return { milestones: parsedMilestones };
            }),
          );
        }),
        map(({ milestones }) => this.scheduleMilestones(milestones)),
        map((finalMilestones) => this.sortMilestones(finalMilestones)),
        map((sortedMilestones) => this.filterMilestonesInView(sortedMilestones)),
        tap((finalMilestones) => {
          this.openMilestones = finalMilestones;
        }),
        catchError((error) => {
          this.toastrService.error('Could not load roadmap data.', 'Error');
          console.error(error);
          return of(null);
        }),
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
          setTimeout(() => this.scrollToToday(), 0);
        }),
      )
      .subscribe();
  }

  private scheduleMilestones(milestones: Milestone[]): Milestone[] {
    // Parse versions
    const parsed = milestones
      .map((m) => ({ milestone: m, version: this.parseVersion(m.title) }))
      .filter((mv) => mv.version);

    // Majors en minors splitsen
    const majors = parsed.filter((mv) => mv.version!.patch === 0).sort((a, b) => {
      if (a.version!.major !== b.version!.major) return a.version!.major - b.version!.major;
      return a.version!.minor - b.version!.minor;
    });
    const minors = parsed.filter((mv) => mv.version!.patch > 0);

    // Zoek alle dueOns van majors
    const majorDueOns = majors
      .filter(mv => mv.milestone.dueOn)
      .map(mv => ({
        version: mv.version!,
        dueOn: new Date(mv.milestone.dueOn!)
      }));

    // Huidig kwartaal als fallback
    const today = new Date();
    const currentQuarter = this.getQuarterFromDate(today);

    // Majors plannen
    let majorQuarterMap = new Map<string, Date>(); // key: major.minor, value: Date
    // Eerst majors met dueOn, daarna terugrekenen/opvullen
    // 1. Majors met dueOn: direct zetten
    for (const { milestone, version } of majors) {
      if (!version) continue;
      if (milestone.dueOn) {
        const q = this.getQuarterFromDate(new Date(milestone.dueOn));
        majorQuarterMap.set(`${version.major}.${version.minor}`, q);
      }
    }
    // 2. Majors zonder dueOn: vooruit/achteruit plannen
    // Sorteer majors oplopend
    for (let i = 0; i < majors.length; i++) {
      const { milestone, version } = majors[i];
      if (!version) continue;
      const key = `${version.major}.${version.minor}`;
      if (!majorQuarterMap.has(key)) {
        // Zoek volgende major met dueOn
        let nextWithDueOnIdx = -1;
        for (let j = i + 1; j < majors.length; j++) {
          const nextVersion = majors[j].version;
          if (!nextVersion) continue;
          const nextKey = `${nextVersion.major}.${nextVersion.minor}`;
          if (majorQuarterMap.has(nextKey)) {
            nextWithDueOnIdx = j;
            break;
          }
        }
        if (nextWithDueOnIdx !== -1) {
          // Terugrekenen vanaf volgende dueOn
          const nextVersion = majors[nextWithDueOnIdx].version;
          if (!nextVersion) continue;
          let q = new Date(majorQuarterMap.get(`${nextVersion.major}.${nextVersion.minor}`)!);
          q.setMonth(q.getMonth() - 3 * (nextWithDueOnIdx - i));
          majorQuarterMap.set(key, q);
        } else {
          // Geen volgende dueOn: vooruit plannen vanaf laatste bekende of huidig kwartaal
          let startQ: Date;
          if (majorQuarterMap.size > 0) {
            // Pak laatste geplande major
            const lastPlanned = majors.slice(0, i).reverse().find(mv => mv.version && majorQuarterMap.has(`${mv.version.major}.${mv.version.minor}`));
            if (lastPlanned && lastPlanned.version) {
              const lastQ = new Date(majorQuarterMap.get(`${lastPlanned.version.major}.${lastPlanned.version.minor}`)!);
              startQ = new Date(lastQ);
              startQ.setMonth(startQ.getMonth() + 3 * (i - majors.indexOf(lastPlanned)));
            } else {
              startQ = new Date(currentQuarter);
            }
          } else {
            startQ = new Date(currentQuarter);
          }
          majorQuarterMap.set(key, startQ);
        }
      }
    }
    // Zet dueOn op einde van kwartaal
    for (const { milestone, version } of majors) {
      if (!version) continue;
      const key = `${version.major}.${version.minor}`;
      const q = majorQuarterMap.get(key)!;
      milestone.dueOn = this.getQuarterEndDate(q);
      milestone.isEstimated = !milestone.dueOn;
    }

    // Zoek earliest dueOn van alle majors
    let earliestMajorQuarter: Date | null = null;
    for (const { milestone, version } of majors) {
      if (!version) continue;
      if (milestone.dueOn) {
        const q = this.getQuarterFromDate(new Date(milestone.dueOn));
        if (!earliestMajorQuarter || q < earliestMajorQuarter) {
          earliestMajorQuarter = q;
        }
      }
    }
    // Fallback: als geen dueOn, gebruik huidig kwartaal
    if (!earliestMajorQuarter) {
      earliestMajorQuarter = new Date(currentQuarter);
    }

    // Minors per major plannen
    const minorsByMajor = new Map<string, { milestone: Milestone; version: Version }[]>();
    for (const mv of minors) {
      const key = `${mv.version!.major}.${mv.version!.minor}`;
      if (!minorsByMajor.has(key)) minorsByMajor.set(key, []);
      minorsByMajor.get(key)!.push(mv as { milestone: Milestone; version: Version });
    }
    for (const [key, minorList] of minorsByMajor) {
      // Sorteer minors oplopend
      minorList.sort((a, b) => a.version.patch - b.version.patch);
      // Zoek dueOn van laatste minor (indien aanwezig)
      let lastMinorWithDueOnIdx = -1;
      for (let i = minorList.length - 1; i >= 0; i--) {
        if (minorList[i].milestone.dueOn) {
          lastMinorWithDueOnIdx = i;
          break;
        }
      }
      if (lastMinorWithDueOnIdx !== -1) {
        // Terugrekenen vanaf laatste dueOn
        let q = this.getQuarterFromDate(new Date(minorList[lastMinorWithDueOnIdx].milestone.dueOn!));
        for (let i = lastMinorWithDueOnIdx; i >= 0; i--) {
          minorList[i].milestone.dueOn = this.getQuarterEndDate(q);
          minorList[i].milestone.isEstimated = !minorList[i].milestone.dueOn;
          q.setMonth(q.getMonth() - 3);
        }
        // Vooruit plannen voor minors na de laatste met dueOn
        q = this.getQuarterFromDate(new Date(minorList[lastMinorWithDueOnIdx].milestone.dueOn!));
        for (let i = lastMinorWithDueOnIdx + 1; i < minorList.length; i++) {
          q.setMonth(q.getMonth() + 3);
          minorList[i].milestone.dueOn = this.getQuarterEndDate(q);
          minorList[i].milestone.isEstimated = !minorList[i].milestone.dueOn;
        }
      } else {
        // Geen dueOn: plan eerste minor in earliestMajorQuarter, elke volgende in het volgende kwartaal
        let q = new Date(earliestMajorQuarter);
        for (let i = 0; i < minorList.length; i++) {
          minorList[i].milestone.dueOn = this.getQuarterEndDate(q);
          minorList[i].milestone.isEstimated = !minorList[i].milestone.dueOn;
          q.setMonth(q.getMonth() + 3);
        }
      }
    }

    // Uniek houden: geen dubbele milestones per versie
    const uniqueMilestones = new Map<string, Milestone>();
    for (const { milestone, version } of parsed) {
      if (version) {
        uniqueMilestones.set(`${version.major}.${version.minor}.${version.patch}`, milestone);
      }
    }

    return [...uniqueMilestones.values()];
  }

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

  private filterMilestonesInView(milestones: Milestone[]): Milestone[] {
    if (!this.timelineStartDate || !this.timelineEndDate) return milestones;
    const viewStartTime = this.timelineStartDate.getTime();
    const viewEndTime = this.timelineEndDate.getTime();

    return milestones.filter((milestone) => {
      if (!milestone.dueOn) return false;
      const dueDate = new Date(milestone.dueOn);
      const issues = this.getIssuesForMilestone(milestone.id);
      const hasOpen = issues.some(issue => issue.state === GitHubStates.OPEN);

      // 1. Altijd tonen in het kwartaal waarin dueOn valt
      const dueQuarterStart = this.getQuarterFromDate(dueDate);
      const dueQuarterEnd = this.getQuarterEndDate(dueQuarterStart);
      if (dueQuarterStart.getTime() >= viewStartTime && dueQuarterStart.getTime() <= viewEndTime) {
        return true;
      }

      // 2. Uitloop: milestone is gepland in het verleden, maar heeft nog open issues
      // Toon deze milestone in het eerste kwartaal NA dueOn waarin de timeline start
      if (hasOpen && dueQuarterStart.getTime() < viewStartTime) {
        // Bepaal het eerste kwartaal na dueOn
        const firstFutureQuarterStart = new Date(dueQuarterStart);
        firstFutureQuarterStart.setMonth(firstFutureQuarterStart.getMonth() + 3);
        const firstFutureQuarterEnd = this.getQuarterEndDate(firstFutureQuarterStart);
        // Als de timeline start in of na het eerste kwartaal na dueOn, toon de milestone
        if (viewStartTime >= firstFutureQuarterStart.getTime() && viewStartTime <= firstFutureQuarterEnd.getTime()) {
          return true;
        }
      }

      // 3. Anders: niet tonen
      return false;
    });
  }

  private scrollToToday(): void {
    if (this.viewInitialized && this.scrollContainer?.nativeElement && this.todayOffsetPercentage > 0) {
      const container = this.scrollContainer.nativeElement;
      const titleWidth = window.innerWidth >= 1024 ? 300 : 150;

      const scrollableWidth = container.scrollWidth - container.clientWidth;
      const timelineAreaWidth = container.scrollWidth - titleWidth;
      const todayMarkerAbsolutePosition = (this.todayOffsetPercentage / 100) * timelineAreaWidth;
      const desiredScrollLeft = todayMarkerAbsolutePosition - container.clientWidth / 3;

      container.scrollLeft = Math.max(0, Math.min(desiredScrollLeft, scrollableWidth));
    }
  }

  private parseMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.map((m) => ({ ...m, dueOn: m.dueOn ? new Date(m.dueOn) : null }));
  }

  private sortMilestones(milestones: Milestone[]): Milestone[] {
    return milestones.sort((a, b) => {
      // Eerst op dueOn (oud -> nieuw, zonder dueOn laatst)
      const aDue = a.dueOn ? new Date(a.dueOn).getTime() : Number.MAX_SAFE_INTEGER;
      const bDue = b.dueOn ? new Date(b.dueOn).getTime() : Number.MAX_SAFE_INTEGER;
      if (aDue !== bDue) return aDue - bDue;
      // Binnen dueOn: majors (patch==0) altijd boven minors (patch>0)
      const aMatch = a.title.match(/(\d+)\.(\d+)\.(\d+)/);
      const bMatch = b.title.match(/(\d+)\.(\d+)\.(\d+)/);
      if (aMatch && bMatch) {
        const aPatch = Number(aMatch[3]);
        const bPatch = Number(bMatch[3]);
        if (aPatch === 0 && bPatch > 0) return -1;
        if (aPatch > 0 && bPatch === 0) return 1;
        // Binnen major/minor: oplopend op release nummer
        const aMajor = Number(aMatch[1]);
        const bMajor = Number(bMatch[1]);
        if (aMajor !== bMajor) return aMajor - bMajor;
        const aMinor = Number(aMatch[2]);
        const bMinor = Number(bMatch[2]);
        if (aMinor !== bMinor) return aMinor - bMinor;
        if (aPatch !== bPatch) return aPatch - bPatch;
      }
      // Fallback: alfabetisch
      return a.title.localeCompare(b.title);
    });
  }

  private getQuarterFromDate(date: Date): Date {
    const year = date.getFullYear();
    const quarterIndex = Math.floor(date.getMonth() / 3);
    return new Date(year, quarterIndex * 3, 1);
  }

  private getQuarterEndDate(quarterStart: Date): Date {
    const quarterEnd = new Date(quarterStart);
    quarterEnd.setMonth(quarterEnd.getMonth() + 3);
    quarterEnd.setDate(0); // Last day of the month
    quarterEnd.setHours(23, 59, 59, 999);
    return quarterEnd;
  }

  private buildIssuesPerQuarterMap(): void {
    this.issuesPerQuarter.clear();
    const today = new Date();
    const currentQuarterStart = this.getQuarterFromDate(today);

    // Maak een lijst van alle quarters in de huidige view
    const quarterNames = this.quarters.map(q => q.name);
    const quarterDates = this.quarters.map(q => {
      // Parse quarter name 'Q2 2025' => Date object
      const [qLabel, year] = q.name.split(' ');
      const quarterNum = Number(qLabel.replace('Q', ''));
      return new Date(Number(year), (quarterNum - 1) * 3, 1);
    });

    interface VersionedMilestone {
      milestone: Milestone;
      version: { major: number; minor: number; patch: number };
      dueOn: Date | null;
    }
    const versioned: VersionedMilestone[] = this.openMilestones
      .map(m => {
        const match = m.title.match(/(\d+)\.(\d+)\.(\d+)/);
        if (!match) return null;
        return {
          milestone: m,
          version: {
            major: Number(match[1]),
            minor: Number(match[2]),
            patch: Number(match[3]),
          },
          dueOn: m.dueOn ? new Date(m.dueOn) : null,
        };
      })
      .filter(Boolean) as VersionedMilestone[];

    // Majors/minors plannen (zoals nu)
    // ... bestaande code ...

    // Issues per kwartaal/milestone
    for (const v of versioned) {
      const milestone = v.milestone;
      const issues = this.getIssuesForMilestone(milestone.id);
      const milestoneQuarter = milestone.dueOn ? this.getQuarterFromDate(new Date(milestone.dueOn)) : currentQuarterStart;
      // Closed issues per kwartaal (altijd in kwartaal van closed_at)
      for (const issue of issues) {
        if (issue.state === GitHubStates.CLOSED && issue.closedAt) {
          // Zoek de juiste quarter key
          const closedQuarter = this.getQuarterFromDate(new Date(issue.closedAt));
          const closedQuarterKey = this.getQuarterKey(closedQuarter);
          if (!this.issuesPerQuarter.has(closedQuarterKey)) this.issuesPerQuarter.set(closedQuarterKey, []);
          let entry = this.issuesPerQuarter.get(closedQuarterKey)!.find(e => e.milestone.id === milestone.id);
          if (!entry) {
            entry = { milestone, issues: [] };
            this.issuesPerQuarter.get(closedQuarterKey)!.push(entry);
          }
          // Voeg alleen toe als nog niet aanwezig
          if (!entry.issues.some(i => i.id === issue.id)) {
            entry.issues.push(issue);
          }
        }
      }
      // Open issues per kwartaal
      const openIssues = issues.filter(i => i.state === GitHubStates.OPEN);
      for (const issue of openIssues) {
        if (milestoneQuarter < currentQuarterStart) {
          // Milestone in het verleden: issue in elk kwartaal NA dueOn t/m huidige kwartaal
          for (let i = 0; i < quarterDates.length; i++) {
            const qDate = quarterDates[i];
            if (qDate > milestoneQuarter && qDate <= currentQuarterStart) {
              const qKey = quarterNames[i];
              if (!this.issuesPerQuarter.has(qKey)) this.issuesPerQuarter.set(qKey, []);
              let entry = this.issuesPerQuarter.get(qKey)!.find(e => e.milestone.id === milestone.id);
              if (!entry) {
                entry = { milestone, issues: [] };
                this.issuesPerQuarter.get(qKey)!.push(entry);
              }
              if (!entry.issues.some(i2 => i2.id === issue.id)) {
                entry.issues.push(issue);
              }
            }
          }
        } else {
          // Milestone in huidig/toekomstig kwartaal: alleen in hun eigen kwartaal
          const openQuarterKey = this.getQuarterKey(milestoneQuarter);
          if (!this.issuesPerQuarter.has(openQuarterKey)) this.issuesPerQuarter.set(openQuarterKey, []);
          let entry = this.issuesPerQuarter.get(openQuarterKey)!.find(e => e.milestone.id === milestone.id);
          if (!entry) {
            entry = { milestone, issues: [] };
            this.issuesPerQuarter.get(openQuarterKey)!.push(entry);
          }
          if (!entry.issues.some(i2 => i2.id === issue.id)) {
            entry.issues.push(issue);
          }
        }
      }
    }
  }

  private getQuarterKey(quarter: Date): string {
    const year = quarter.getFullYear();
    const quarterNumber = Math.floor(quarter.getMonth() / 3) + 1;
    return `Q${quarterNumber} ${year}`;
  }
}
