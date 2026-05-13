import { Pipe, PipeTransform } from '@angular/core';

/**
 * Maps a CVSS severity string ('CRITICAL', 'HIGH', …) or an impact score (number)
 * to a shared badge CSS class: badge-critical | badge-high | badge-medium | badge-low | badge-none.
 *
 * Usage:
 *   <span class="badge" [ngClass]="'CRITICAL' | badgeClass">CRITICAL</span>
 *   <span class="badge" [ngClass]="7.5 | badgeClass">Critical</span>
 */
@Pipe({ name: 'badgeClass', standalone: true, pure: true })
export class BadgeClassPipe implements PipeTransform {
  transform(value: string | number | null | undefined): string {
    if (value == null) return 'badge-none';

    if (typeof value === 'number') {
      if (value >= 7.5) return 'badge-critical';
      if (value >= 5) return 'badge-high';
      if (value >= 2.5) return 'badge-medium';
      return 'badge-low';
    }

    switch (value.toUpperCase()) {
      case 'CRITICAL': {
        return 'badge-critical';
      }
      case 'HIGH': {
        return 'badge-high';
      }
      case 'MEDIUM': {
        return 'badge-medium';
      }
      case 'LOW': {
        return 'badge-low';
      }
      default: {
        return 'badge-none';
      }
    }
  }
}
