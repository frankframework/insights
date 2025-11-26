import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Release, ReleaseService } from '../../services/release.service';
import { ReleaseBusinessValueComponent } from '../release-details/release-business-value/release-business-value.component';
import { ReleaseImportantIssuesComponent } from '../release-details/release-important-issues/release-important-issues.component';
import { ReleaseVulnerabilities } from '../release-details/release-vulnerabilities/release-vulnerabilities';
import { Issue, IssueService } from '../../services/issue.service';
import { Vulnerability, VulnerabilityService } from '../../services/vulnerability.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-release-manage',
  standalone: true,
  imports: [
    CommonModule,
    ReleaseBusinessValueComponent,
    ReleaseImportantIssuesComponent,
    ReleaseVulnerabilities,
  ],
  templateUrl: './release-manage.component.html',
  styleUrl: './release-manage.component.scss',
})
export class ReleaseManageComponent implements OnInit {
  public authService = inject(AuthService);
  public release = signal<Release | null>(null);
  public releaseIssues = signal<Issue[] | undefined>([]);
  public vulnerabilities = signal<Vulnerability[] | undefined>([]);
  public isLoading = signal<boolean>(true);
  public activeSection = signal<'business-value' | 'vulnerabilities' | null>(null);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private releaseService = inject(ReleaseService);
  private issueService = inject(IssueService);
  private vulnerabilityService = inject(VulnerabilityService);

  ngOnInit(): void {
    const releaseId = this.route.snapshot.paramMap.get('id');
    if (releaseId) {
      this.fetchReleaseData(releaseId);
    }
  }

  public goBack(): void {
    const releaseId = this.release()?.id;
    if (releaseId) {
      this.router.navigate(['/graph', releaseId]);
    } else {
      this.router.navigate(['/graph']);
    }
  }

  public openSection(section: 'business-value' | 'vulnerabilities'): void {
    const releaseId = this.release()?.id;
    if (!releaseId) return;

    if (section === 'business-value') {
      this.router.navigate(['/release-manage', releaseId, 'business-values']);
    } else {
      this.activeSection.set(section);
    }
  }

  public closeSection(): void {
    this.activeSection.set(null);
  }

  private fetchReleaseData(releaseId: string): void {
    this.isLoading.set(true);

    this.releaseService.getReleaseById(releaseId).subscribe({
      next: (release) => {
        this.release.set(release);
        this.fetchIssues(releaseId);
        this.fetchVulnerabilities(releaseId);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.router.navigate(['/not-found']);
      },
    });
  }

  private fetchIssues(releaseId: string): void {
    this.issueService
      .getIssuesByReleaseId(releaseId)
      .pipe(
        catchError(() => {
          this.releaseIssues.set(undefined);
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
          this.vulnerabilities.set(undefined);
          return of();
        }),
      )
      .subscribe((vulnerabilities) => {
        this.vulnerabilities.set(vulnerabilities);
      });
  }
}
