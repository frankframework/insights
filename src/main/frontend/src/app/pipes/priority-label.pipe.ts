import { Pipe, PipeTransform } from '@angular/core';
import { getImpactPriorityLabel } from '../services/vulnerability.service';

/**
 * Transforms an impact score into a human-readable priority label.
 *
 * Usage:  {{ 7.5 | priorityLabel }}  →  'Critical'
 */
@Pipe({ name: 'priorityLabel', standalone: true, pure: true })
export class PriorityLabelPipe implements PipeTransform {
  transform(score: number | null | undefined): string {
    return getImpactPriorityLabel(score);
  }
}
