import { Component, DestroyRef, OnInit, Signal, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { catchError, finalize, forkJoin, of, switchMap } from 'rxjs';
import { Release, ReleaseService } from '../../services/release.service';
import { Label, LabelService } from '../../services/label.service';
import { Issue, IssueService } from '../../services/issue.service';
import { Vulnerability, VulnerabilityService } from '../../services/vulnerability.service';
import { AuthService } from '../../services/auth.service';
import { GraphStateService } from '../../services/graph-state.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { ReleaseHighlightsComponent } from './release-highlights/release-highlights.component';
import { ReleaseImportantIssuesComponent } from './release-important-issues/release-important-issues.component';
import { ReleaseBusinessValueComponent } from './release-business-value/release-business-value.component';
import { ReleaseVulnerabilities } from './release-vulnerabilities/release-vulnerabilities';
import { ActivatedRoute, Params, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BusinessValue, BusinessValueService } from '../../services/business-value.service';

@Component({
  selector: 'app-release-details',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    LoaderComponent,
    ReleaseHighlightsComponent,
    ReleaseImportantIssuesComponent,
    ReleaseBusinessValueComponent,
    ReleaseVulnerabilities,
  ],
  templateUrl: './release-details.component.html',
})
export class ReleaseDetailsComponent implements OnInit {
  public readonly authService = inject(AuthService);
  public readonly graphStateService = inject(GraphStateService);

  public readonly release = signal<Release | null>(null);
  public readonly highlightedLabels = signal<Label[] | null>(null);
  public readonly releaseIssues = signal<Issue[] | null>(null);
  public readonly vulnerabilities = signal<Vulnerability[] | null>(null);
  public readonly businessValues = signal<BusinessValue[] | null>(null);
  public readonly isLoading = signal<boolean>(true);
  public readonly activeView = signal<'business-value' | 'issues'>('issues');

  public readonly previousRelease = signal<Release | null>(null);
  public readonly nextRelease = signal<Release | null>(null);
  public readonly branchReleases = signal<Release[]>([]);

  public readonly graphQueryParams: Signal<Params> = computed(() => this.graphStateService.graphQueryParams());

  private readonly releaseService = inject(ReleaseService);
  private readonly labelService = inject(LabelService);
  private readonly issueService = inject(IssueService);
  private readonly vulnerabilityService = inject(VulnerabilityService);
  private readonly businessValueService = inject(BusinessValueService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((parameters) => {
          const releaseId = parameters.get('id');
          if (!releaseId) {
            return of(null);
          }
          this.isLoading.set(true);
          return forkJoin({
            release: this.releaseService.getReleaseById(releaseId),
            allReleases: this.releaseService.getAllReleases().pipe(catchError(() => of([]))),
          }).pipe(
            catchError((error) => {
              console.error('Failed to load release:', error);
              this.isLoading.set(false);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        if (result) {
          this.release.set(result.release);
          this.computeBranchNavigation(result.release, result.allReleases);
          this.fetchData(result.release.id);
        }
      });
  }

  public setActiveView(view: 'business-value' | 'issues'): void {
    this.activeView.set(view);
  }

  public releaseGraphLink(release: Release | null): string {
    if (!release) return '';
    return `/graph/${release.tagName.replace(/^release\//, '')}`;
  }

  private isNightly(release: Release): boolean {
    return release.tagName.replace(/^release\//, '').includes('nightly');
  }

  private computeBranchNavigation(release: Release, allReleases: Release[]): void {
    const sorted = allReleases
      .filter((r) => r.branch.name === release.branch.name)
      .toSorted((a, b) => new Date(a.publishedAt).getTime() - new Date(b.publishedAt).getTime());

    const filtered = sorted.filter((r, index_) => {
      if (!this.isNightly(r)) return true;
      return !sorted.slice(index_ + 1).some((next) => !this.isNightly(next));
    });

    const index = filtered.findIndex((r) => r.id === release.id);

    this.branchReleases.set(filtered);
    this.previousRelease.set(index > 0 ? filtered[index - 1] : null);
    this.nextRelease.set(index < filtered.length - 1 ? filtered[index + 1] : null);
  }

  private fetchData(releaseId: string): void {
    const labels$ = this.labelService.getHighLightsByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load highlights:', error);
        return of([]);
      }),
    );

    const issues$ = this.issueService.getIssuesByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load issues:', error);
        return of([]);
      }),
    );

    const vulnerabilities$ = this.vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load vulnerabilities:', error);
        return of([]);
      }),
    );

    const businessValues$ = this.businessValueService.getBusinessValuesByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load businessValues:', error);
        return of([]);
      }),
    );

    forkJoin({
      labels: labels$,
      issues: issues$,
      vulnerabilities: vulnerabilities$,
      businessValues: businessValues$,
    })
      .pipe(
        finalize(() => {
          this.isLoading.set(false);
        }),
      )
      .subscribe(({ labels, issues, vulnerabilities, businessValues }) => {
        this.highlightedLabels.set(labels.length > 0 ? labels : null);
        this.releaseIssues.set(issues.length > 0 ? issues : null);
        this.vulnerabilities.set(vulnerabilities);
        this.businessValues.set(businessValues.length > 0 ? businessValues : null);
        this.activeView.set(businessValues.length > 0 ? 'business-value' : 'issues');
      });
  }
}
