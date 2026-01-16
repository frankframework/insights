import { Component, Input, OnChanges } from '@angular/core';
import { Vulnerability, VulnerabilitySeverities, VulnerabilitySeverity } from '../../../services/vulnerability.service';
import { CommonModule } from '@angular/common';
import { VulnerabilityDetailsOffCanvas } from '../vulnerability-details-off-canvas/vulnerability-details-off-canvas';

@Component({
  selector: 'app-release-vulnerabilities',
  standalone: true,
  imports: [CommonModule, VulnerabilityDetailsOffCanvas],
  templateUrl: './release-vulnerabilities.html',
  styleUrl: './release-vulnerabilities.scss',
})
export class ReleaseVulnerabilities implements OnChanges {
  @Input() vulnerabilities: Vulnerability[] = [];
  @Input() lastScanned: Date | null = null;

  public sortedVulnerabilities: Vulnerability[] = [];
  public selectedVulnerability: Vulnerability | null = null;
  public isOffCanvasOpen = false;

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
    this.selectedVulnerability = null;
    this.isOffCanvasOpen = false;
  }

  public selectVulnerability(vulnerability: Vulnerability): void {
    this.selectedVulnerability = vulnerability;
    this.isOffCanvasOpen = true;
  }

  public closeOffCanvas(): void {
    this.isOffCanvasOpen = false;
  }

  public getSeverityClass(severity: VulnerabilitySeverity): string {
    return `severity-${severity.toLowerCase()}`;
  }

  public formatCvssScore(score: number): string {
    return score % 1 === 0 ? score.toString() : score.toFixed(1);
  }

  private sortVulnerabilities(): void {
    this.sortedVulnerabilities = [...this.vulnerabilities].toSorted((a, b) => {
      const orderA = this.severityOrder[a.severity] || 999;
      const orderB = this.severityOrder[b.severity] || 999;
      if (orderA !== orderB) {
        return orderA - orderB;
      }

      return b.cvssScore - a.cvssScore;
    });
  }
}
