import { Pipe, PipeTransform } from '@angular/core';

/**
 * Transforms an impact score into a readable impact label.
 *
 * Usage:  {{ 7.5 | impactLabel }}  →  'Critical'
 */
@Pipe({ name: 'impactLabel', standalone: true, pure: true })
export class ImpactLabelPipe implements PipeTransform {
  transform(score: number | null | undefined): string {
    return getImpactLabel(score);
  }
}

/**
 * Single source of truth for impact labels. Maps an impact score to a
 * readable label, shared by templates and typescript code.
 */
export function getImpactLabel(impactScore: number | null | undefined): string {
  if (impactScore === null || impactScore === undefined) return 'Not assessed';
  if (impactScore >= 7.5) return 'Critical';
  if (impactScore >= 5) return 'High';
  if (impactScore >= 2.5) return 'Medium';
  if (impactScore > 0) return 'Low';
  return 'No';
}
