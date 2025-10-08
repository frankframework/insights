import { Component, Input, OnChanges } from '@angular/core';
import { Vulnerability, VulnerabilitySeverities, VulnerabilitySeverity } from '../../../services/vulnerability.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-release-vulnerabilities',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './release-vulnerabilities.html',
  styleUrl: './release-vulnerabilities.scss'
})
export class ReleaseVulnerabilities implements OnChanges {
  @Input() vulnerabilities: Vulnerability[] = [];

  public sortedVulnerabilities: Vulnerability[] = [];
  public selectedVulnerability: Vulnerability | null = null;

  private severityOrder: Record<VulnerabilitySeverity, number> = {
    [VulnerabilitySeverities.CRITICAL]: 1,
    [VulnerabilitySeverities.HIGH]: 2,
    [VulnerabilitySeverities.MEDIUM]: 3,
    [VulnerabilitySeverities.LOW]: 4,
    [VulnerabilitySeverities.NONE]: 5,
    [VulnerabilitySeverities.UNKNOWN]: 6
  };

  ngOnChanges(): void {
    this.sortVulnerabilities();
    if (this.sortedVulnerabilities.length > 0) {
      this.selectedVulnerability = this.sortedVulnerabilities[0];
    }
  }

  public selectVulnerability(vulnerability: Vulnerability): void {
    this.selectedVulnerability = vulnerability;
  }

  public getSeverityClass(severity: VulnerabilitySeverity): string {
    return `severity-${severity.toLowerCase()}`;
  }

  private sortVulnerabilities(): void {
    this.sortedVulnerabilities = [...this.vulnerabilities].sort((a, b) => {
      const orderA = this.severityOrder[a.severity] || 999;
      const orderB = this.severityOrder[b.severity] || 999;
      if (orderA !== orderB) {
        return orderA - orderB;
      }
      return a.cveId.localeCompare(b.cveId);
    });
  }
}
