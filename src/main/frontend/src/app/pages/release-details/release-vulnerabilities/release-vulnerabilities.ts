import { Component, Input, OnChanges } from '@angular/core';
import { Vulnerability, VulnerabilitySeverities, VulnerabilitySeverity } from '../../../services/vulnerability.service';
import { CommonModule } from '@angular/common';

const easeInOutCubic = (t: number): number => {
  return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
};

@Component({
  selector: 'app-release-vulnerabilities',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './release-vulnerabilities.html',
  styleUrl: './release-vulnerabilities.scss',
})
export class ReleaseVulnerabilities implements OnChanges {
  @Input() vulnerabilities: Vulnerability[] = [];

  public sortedVulnerabilities: Vulnerability[] = [];
  public selectedVulnerability: Vulnerability | null = null;
  public isDescriptionExpanded = false;
  public showSeeMoreButton = false;

  private severityOrder: Record<VulnerabilitySeverity, number> = {
    [VulnerabilitySeverities.CRITICAL]: 1,
    [VulnerabilitySeverities.HIGH]: 2,
    [VulnerabilitySeverities.MEDIUM]: 3,
    [VulnerabilitySeverities.LOW]: 4,
    [VulnerabilitySeverities.NONE]: 5,
    [VulnerabilitySeverities.UNKNOWN]: 6,
  };

  ngOnChanges(): void {
    this.sortVulnerabilities();
    if (this.sortedVulnerabilities.length > 0) {
      this.selectedVulnerability = this.sortedVulnerabilities[0];
    }
    this.isDescriptionExpanded = false;
    this.checkDescriptionOverflow();
  }

  public selectVulnerability(vulnerability: Vulnerability): void {
    this.selectedVulnerability = vulnerability;
    this.isDescriptionExpanded = false;

    this.smoothScrollToTop();
    this.checkDescriptionOverflow();
  }

  public toggleDescription(): void {
    this.isDescriptionExpanded = !this.isDescriptionExpanded;
  }

  public getSeverityClass(severity: VulnerabilitySeverity): string {
    return `severity-${severity.toLowerCase()}`;
  }

  public getCweUrl(cwe: string): string {
    const cweNumber = cwe.match(/\d+/)?.[0];
    return cweNumber ? `https://cwe.mitre.org/data/definitions/${cweNumber}.html` : '#';
  }

  public formatCvssScore(score: number): string {
    return score % 1 === 0 ? score.toString() : score.toFixed(1);
  }

  private smoothScrollToTop(): void {
    const cweDetails = document.querySelector('.cwe-details') as HTMLElement;
    if (!cweDetails) {
      return;
    }

    const offset = 250;
    const targetPosition = cweDetails.getBoundingClientRect().top + window.pageYOffset - offset;
    const start = window.pageYOffset;
    const distance = targetPosition - start;
    const duration = 800;
    const startTime = performance.now();

    const scroll = (currentTime: number): void => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const easeProgress = easeInOutCubic(progress);

      window.scrollTo(0, start + distance * easeProgress);

      if (progress < 1) {
        requestAnimationFrame(scroll);
      }
    };

    requestAnimationFrame(scroll);
  }

  private checkDescriptionOverflow(): void {
    const descriptionElement = document.querySelector('.cve-description') as HTMLElement;
    this.showSeeMoreButton = descriptionElement
      ? descriptionElement.scrollHeight > descriptionElement.clientHeight
      : false;
  }

  private sortVulnerabilities(): void {
    this.sortedVulnerabilities = [...this.vulnerabilities].sort((a, b) => {
      const orderA = this.severityOrder[a.severity] || 999;
      const orderB = this.severityOrder[b.severity] || 999;
      if (orderA !== orderB) {
        return orderA - orderB;
      }

      return b.cvssScore - a.cvssScore;
    });
  }
}
