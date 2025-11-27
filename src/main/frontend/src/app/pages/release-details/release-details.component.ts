import { Component, inject, OnInit, signal } from '@angular/core';
import { Location, CommonModule } from '@angular/common';
import { catchError, finalize, forkJoin, of, switchMap } from 'rxjs';
import { Release, ReleaseService } from '../../services/release.service';
import { Label, LabelService } from '../../services/label.service';
import { Issue, IssueService } from '../../services/issue.service';
import { Vulnerability, VulnerabilityService } from '../../services/vulnerability.service';
import { LoaderComponent } from '../../components/loader/loader.component';
import { ReleaseHighlightsComponent } from './release-highlights/release-highlights.component';
import { ReleaseImportantIssuesComponent } from './release-important-issues/release-important-issues.component';
import { ReleaseBusinessValueComponent } from './release-business-value/release-business-value.component';
import { ReleaseVulnerabilities } from './release-vulnerabilities/release-vulnerabilities';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-release-details',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
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
  public highlightedLabels?: Label[];
  public releaseIssues?: Issue[];
  public vulnerabilities?: Vulnerability[];
  public isLoading = true;
  public showBusinessValue = signal<boolean>(true);
  public showImportantIssues = signal<boolean>(false);

  private location = inject(Location);
  private releaseService = inject(ReleaseService);
  private labelService = inject(LabelService);
  private issueService = inject(IssueService);
  private vulnerabilityService = inject(VulnerabilityService);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((parameters) => {
          const releaseId = parameters.get('id');
          if (!releaseId) {
            return of(null);
          }
          this.isLoading = true;
          return this.releaseService.getReleaseById(releaseId).pipe(
            catchError((error) => {
              console.error('Failed to load release:', error);
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

  public toggleBusinessValue(): void {
    this.showBusinessValue.set(!this.showBusinessValue());
  }

  public toggleImportantIssues(): void {
    this.showImportantIssues.set(!this.showImportantIssues());
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

    forkJoin({
      labels: labels$,
      issues: issues$,
      vulnerabilities: vulnerabilities$,
    })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe(({ labels, issues, vulnerabilities }) => {
        this.highlightedLabels = labels && labels.length > 0 ? labels : undefined;
        this.releaseIssues = issues && issues.length > 0 ? issues : undefined;

        this.vulnerabilities = vulnerabilities && vulnerabilities.length > 0 ? vulnerabilities : [];
      });
  }
}
