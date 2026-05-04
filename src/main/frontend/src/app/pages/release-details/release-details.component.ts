import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
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
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BusinessValue, BusinessValueService } from '../../services/business-value.service';

@Component({
  selector: 'app-release-details',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LoaderComponent,
    ReleaseHighlightsComponent,
    ReleaseImportantIssuesComponent,
    ReleaseBusinessValueComponent,
    ReleaseVulnerabilities,
  ],
  templateUrl: './release-details.component.html',
  styleUrl: './release-details.component.scss',
})
export class ReleaseDetailsComponent implements OnInit {
  public release?: Release;
  public highlightedLabels: Label[] | null = null;
  public releaseIssues: Issue[] | null = null;
  public vulnerabilities: Vulnerability[] | null = null;
  public businessValues: BusinessValue[] | null = null;
  public isLoading = true;
  public activeView = signal<'business-value' | 'issues'>('issues');
  public authService = inject(AuthService);

  public previousRelease = signal<Release | null>(null);
  public nextRelease = signal<Release | null>(null);
  public branchReleases = signal<Release[]>([]);

  private router = inject(Router);
  private releaseService = inject(ReleaseService);
  private labelService = inject(LabelService);
  private issueService = inject(IssueService);
  private vulnerabilityService = inject(VulnerabilityService);
  private businessValueService = inject(BusinessValueService);
  private route = inject(ActivatedRoute);
  private graphStateService = inject(GraphStateService);

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((parameters) => {
          const releaseId = parameters.get('id');
          if (!releaseId) {
            return of(null);
          }
          this.isLoading = true;
          return forkJoin({
            release: this.releaseService.getReleaseById(releaseId),
            allReleases: this.releaseService.getAllReleases().pipe(catchError(() => of([]))),
          }).pipe(
            catchError((error) => {
              console.error('Failed to load release:', error);
              this.isLoading = false;
              return of(null);
            }),
          );
        }),
      )
      .subscribe((result) => {
        if (result) {
          this.release = result.release;
          this.computeBranchNavigation(result.release, result.allReleases);
          this.fetchData(result.release.id);
        }
      });
  }

  public goBack(): void {
    const queryParameters = this.graphStateService.getGraphQueryParams();
    this.router.navigate(['/graph'], { queryParams: queryParameters });
  }

  public setActiveView(view: 'business-value' | 'issues'): void {
    this.activeView.set(view);
  }

  public navigateToRelease(release: Release | null): void {
    if (!release) return;
    const identifier = release.tagName.replace(/^release\//, '');
    const queryParameters = this.graphStateService.getGraphQueryParams();
    this.router.navigate(['/graph', identifier], { queryParams: queryParameters });
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
          this.isLoading = false;
        }),
      )
      .subscribe(({ labels, issues, vulnerabilities, businessValues }) => {
        this.highlightedLabels = labels.length > 0 ? labels : null;
        this.releaseIssues = issues.length > 0 ? issues : null;
        this.vulnerabilities = vulnerabilities;
        this.businessValues = businessValues.length > 0 ? businessValues : null;
        this.activeView.set(this.businessValues ? 'business-value' : 'issues');
      });
  }
}
