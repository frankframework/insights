import { Pipe, PipeTransform } from '@angular/core';

/**
 * Transforms an impact score into a readable impact label.
 *
 * Usage:  {{ 7.5 | impactLabel }}  →  'Critical'
 */
@Pipe({ name: 'impactLabel', standalone: true, pure: true })
export class ImpactLabelPipe implements PipeTransform {
  transform(score: number | null | undefined): string {
    if (score === null || score === undefined) return 'Not assessed';
    if (score >= 7.5) return 'Critical';
    if (score >= 5) return 'High';
    if (score >= 2.5) return 'Medium';
    if (score > 0) return 'Low';
    return 'No';
  }
}
