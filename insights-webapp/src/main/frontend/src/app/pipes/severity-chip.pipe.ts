import { Pipe, PipeTransform } from '@angular/core';

export type SeverityKey =
  'severity-critical' | 'severity-high' | 'severity-medium' | 'severity-low' | 'severity-none' | 'severity-unknown';

const SEVERITY_CHIP_CLASSES: Record<SeverityKey, string> = {
  'severity-critical': 'bg-red-100 text-red-800',
  'severity-high': 'bg-orange-200 text-orange-800',
  'severity-medium': 'bg-amber-100 text-amber-800',
  'severity-low': 'bg-blue-100 text-blue-800',
  'severity-none': 'bg-gray-100 text-gray-500',
  'severity-unknown': 'bg-gray-100 text-gray-500',
};

export function toSeverityKey(severity: string | null | undefined): SeverityKey {
  const key = `severity-${(severity ?? 'unknown').toLowerCase()}` as SeverityKey;
  return key in SEVERITY_CHIP_CLASSES ? key : 'severity-unknown';
}

@Pipe({ name: 'severityChip', standalone: true, pure: true })
export class SeverityChipPipe implements PipeTransform {
  transform(severity: string | null | undefined): string {
    const key = toSeverityKey(severity);
    return `${key} ${SEVERITY_CHIP_CLASSES[key]}`;
  }
}
