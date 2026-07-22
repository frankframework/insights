import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Release, ReleaseService } from '../../services/release.service';
import { ReleaseBusinessValueComponent } from '../release-details/release-business-value/release-business-value.component';
import { ReleaseImportantIssuesComponent } from '../release-details/release-important-issues/release-important-issues.component';
import { ReleaseVulnerabilities } from '../release-details/release-vulnerabilities/release-vulnerabilities';
import { Issue, IssueService } from '../../services/issue.service';
import { Vulnerability, VulnerabilityService } from '../../services/vulnerability.service';
import { catchError, finalize, of } from 'rxjs';
import { BusinessValue, BusinessValueService } from '../../services/business-value.service';

@Component({
  selector: 'app-release-manage',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReleaseBusinessValueComponent,
    ReleaseImportantIssuesComponent,
    ReleaseVulnerabilities,
  ],
  templateUrl: './release-manage.component.html',
  styleUrl: './release-manage.component.scss',
})
export class ReleaseManageComponent implements OnInit {
  public static readonly releaseManagePath = '/release-manage';

  public authService = inject(AuthService);
  public release = signal<Release | null>(null);
  public releaseIssues = signal<Issue[] | null>([]);
  public vulnerabilities = signal<Vulnerability[] | null>([]);
  public businessValues = signal<BusinessValue[] | null>([]);
  public isLoading = signal<boolean>(true);
  public activeSection = signal<'business-value' | 'vulnerabilities' | null>(null);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private releaseService = inject(ReleaseService);
  private issueService = inject(IssueService);
  private vulnerabilityService = inject(VulnerabilityService);
  private businessValueService = inject(BusinessValueService);

  ngOnInit(): void {
    const releaseId = this.route.snapshot.paramMap.get('id');
    if (releaseId) {
      this.fetchReleaseData(releaseId);
    }
  }

  public backLink(): string {
    const releaseId = this.release()?.id;
    return releaseId ? `/graph/${releaseId}` : '/graph';
  }

  public businessValueLink(): string {
    const releaseId = this.release()?.id;
    return releaseId ? `/release-manage/${releaseId}/business-values` : '';
  }

  public vulnerabilitiesLink(): string {
    return '/vulnerabilities/manage';
  }

  public vulnerabilitiesLinkQueryParams(): { releaseId: string | null } {
    return { releaseId: this.release()?.id ?? null };
  }

  public closeSection(): void {
    this.activeSection.set(null);
  }

  private fetchReleaseData(releaseId: string): void {
    this.isLoading.set(true);

    this.releaseService
      .getReleaseById(releaseId)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (release) => {
          this.release.set(release);
          this.fetchIssues(releaseId);
          this.fetchVulnerabilities(releaseId);
          this.fetchBusinessValue(releaseId);
        },
        error: () => this.router.navigate(['/not-found']),
      });
  }

  private fetchIssues(releaseId: string): void {
    this.issueService
      .getIssuesByReleaseId(releaseId)
      .pipe(
        catchError(() => {
          this.releaseIssues.set(null);
          return of();
        }),
      )
      .subscribe((issues) => {
        this.releaseIssues.set(issues);
      });
  }

  private fetchVulnerabilities(releaseId: string): void {
    this.vulnerabilityService
      .getVulnerabilitiesByReleaseId(releaseId)
      .pipe(
        catchError(() => {
          this.vulnerabilities.set(null);
          return of();
        }),
      )
      .subscribe((vulnerabilities) => {
        this.vulnerabilities.set(vulnerabilities);
      });
  }

  private fetchBusinessValue(releaseId: string): void {
    this.businessValueService
      .getBusinessValuesByReleaseId(releaseId)
      .pipe(
        catchError(() => {
          this.businessValues.set(null);
          return of();
        }),
      )
      .subscribe((values) => {
        this.businessValues.set(values);
      });
  }
}
