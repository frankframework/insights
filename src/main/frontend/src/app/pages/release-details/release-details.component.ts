import { Component, inject, OnInit } from '@angular/core';
import { Location, CommonModule } from '@angular/common';
import { catchError, finalize, forkJoin, of, switchMap } from 'rxjs';
import { Release, ReleaseService } from '../../services/release.service';
import { Label, LabelService } from '../../services/label.service';
import { Issue, IssueService } from '../../services/issue.service';
import { Vulnerability, VulnerabilityService } from '../../services/vulnerability.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { ReleaseHighlightsComponent } from './release-highlights/release-highlights.component';
import { ReleaseImportantIssuesComponent } from './release-important-issues/release-important-issues.component';
import { ReleaseVulnerabilities } from './release-vulnerabilities/release-vulnerabilities';
import { ToastrService } from 'ngx-toastr';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-release-details',
  standalone: true,
  imports: [CommonModule, LoaderComponent, ReleaseHighlightsComponent, ReleaseImportantIssuesComponent, ReleaseVulnerabilities],
  templateUrl: './release-details.component.html',
  styleUrl: './release-details.component.scss',
})
export class ReleaseDetailsComponent implements OnInit {
  public release?: Release;
  public highlightedLabels?: Label[];
  public releaseIssues?: Issue[];
  public vulnerabilities?: Vulnerability[];
  public isLoading = true;

  private location = inject(Location);
  private releaseService = inject(ReleaseService);
  private labelService = inject(LabelService);
  private issueService = inject(IssueService);
  private vulnerabilityService = inject(VulnerabilityService);
  private toastService = inject(ToastrService);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((parameters) => {
          const releaseId = parameters.get('id');
          if (!releaseId) {
            this.toastService.error('No release ID provided');
            return of(null);
          }
          this.isLoading = true;
          return this.releaseService.getReleaseById(releaseId).pipe(
            catchError((error) => {
              console.error('Failed to load release:', error);
              this.toastService.error('Failed to load release. Please try again later.');
              this.isLoading = false;
              return of(null);
            }),
          );
        }),
      )
      .subscribe((release) => {
        if (release) {
          this.release = release;
          this.fetchData(release.id);
        }
      });
  }

  public goBack(): void {
    this.location.back();
  }

  private fetchData(releaseId: string): void {
    const labels$ = this.labelService.getHighLightsByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load highlights:', error);
        this.toastService.error('Failed to load release highlights. Please try again later.');
        return of();
      }),
    );

    const issues$ = this.issueService.getIssuesByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load issues:', error);
        this.toastService.error('Failed to load release issues. Please try again later.');
        return of();
      }),
    );

    const vulnerabilities$ = this.vulnerabilityService.getVulnerabilitiesByReleaseId(releaseId).pipe(
      catchError((error) => {
        console.error('Failed to load vulnerabilities:', error);
        this.toastService.error('Failed to load vulnerabilities. Please try again later.');
        return of([]);
      }),
    );

    forkJoin({
      labels: labels$,
      issues: issues$,
      vulnerabilities: vulnerabilities$,
    })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe(({ labels, issues, vulnerabilities }) => {
        if (labels && labels.length > 0) {
          this.highlightedLabels = labels;
        } else {
          this.highlightedLabels = undefined;
          if (Array.isArray(labels)) {
            this.toastService.error('No release highlights found.');
          }
        }

        if (issues && issues.length > 0) {
          this.releaseIssues = issues;
        } else {
          this.releaseIssues = undefined;
          if (Array.isArray(issues)) {
            this.toastService.error('No release issues found.');
          }
        }

        if (vulnerabilities && vulnerabilities.length > 0) {
          this.vulnerabilities = vulnerabilities;
        } else {
          this.vulnerabilities = [];
        }
      });
  }
}
