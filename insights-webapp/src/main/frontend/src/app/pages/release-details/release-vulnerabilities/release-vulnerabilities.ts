import { SeverityChipPipe, toSeverityKey } from '../../../pipes/severity-chip.pipe';
import { Component, Signal, WritableSignal, computed, input, linkedSignal } from '@angular/core';
import { Vulnerability, VulnerabilitySeverities, VulnerabilitySeverity } from '../../../services/vulnerability.service';
import { DatePipe, NgClass } from '@angular/common';
import { VulnerabilityDetailsOffCanvas } from '../vulnerability-details-off-canvas/vulnerability-details-off-canvas';

const SEVERITY_ORDER: Record<VulnerabilitySeverity, number> = {
  [VulnerabilitySeverities.CRITICAL]: 1,
  [VulnerabilitySeverities.HIGH]: 2,
  [VulnerabilitySeverities.MEDIUM]: 3,
  [VulnerabilitySeverities.LOW]: 4,
  [VulnerabilitySeverities.NONE]: 5,
  [VulnerabilitySeverities.UNKNOWN]: 6,
};

@Component({
  selector: 'app-release-vulnerabilities',
  standalone: true,
  imports: [SeverityChipPipe, DatePipe, NgClass, VulnerabilityDetailsOffCanvas],
  templateUrl: './release-vulnerabilities.html',
})
export class ReleaseVulnerabilities {
  public readonly vulnerabilities = input<Vulnerability[] | null>(null);
  public readonly lastScanned = input<Date | null>(null);

  public readonly sortedVulnerabilities: Signal<Vulnerability[]> = computed(() =>
    [...(this.vulnerabilities() ?? [])].toSorted((vulnerabilityA, vulnerabilityB) => {
      const orderA = SEVERITY_ORDER[vulnerabilityA.severity] || 999;
      const orderB = SEVERITY_ORDER[vulnerabilityB.severity] || 999;
      if (orderA !== orderB) {
        return orderA - orderB;
      }

      return (vulnerabilityB.cvssScore ?? 0) - (vulnerabilityA.cvssScore ?? 0);
    }),
  );

  public readonly selectedVulnerability: WritableSignal<Vulnerability | null> = linkedSignal<
    Vulnerability[] | null,
    Vulnerability | null
  >({
    source: this.vulnerabilities,
    computation: () => null,
  });

  public readonly isOffCanvasOpen: WritableSignal<boolean> = linkedSignal<Vulnerability[] | null, boolean>({
    source: this.vulnerabilities,
    computation: () => false,
  });

  public selectVulnerability(vulnerability: Vulnerability): void {
    this.selectedVulnerability.set(vulnerability);
    this.isOffCanvasOpen.set(true);
  }

  public closeOffCanvas(): void {
    this.isOffCanvasOpen.set(false);
  }

  public getSeverityClass(severity: VulnerabilitySeverity): string {
    return toSeverityKey(severity);
  }

  public formatCvssScore(score: number | null): string {
    if (score === null) return '—';
    return score % 1 === 0 ? score.toString() : score.toFixed(1);
  }
}
