import { Pipe, PipeTransform } from '@angular/core';

export type BadgeKey = 'badge-critical' | 'badge-high' | 'badge-medium' | 'badge-low' | 'badge-none';

const BADGE_CHIP_CLASSES: Record<BadgeKey, string> = {
  'badge-critical': 'bg-red-100 text-red-800',
  'badge-high': 'bg-orange-200 text-orange-800',
  'badge-medium': 'bg-amber-100 text-amber-800',
  'badge-low': 'bg-blue-100 text-blue-800',
  'badge-none': 'bg-gray-100 text-gray-500',
};

export function toBadgeKey(value: string | number | null | undefined): BadgeKey {
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

@Pipe({ name: 'badgeClass', standalone: true, pure: true })
export class BadgeClassPipe implements PipeTransform {
  transform(value: string | number | null | undefined): string {
    return toBadgeKey(value);
  }
}

@Pipe({ name: 'badgeChip', standalone: true, pure: true })
export class BadgeChipPipe implements PipeTransform {
  transform(value: string | number | null | undefined): string {
    const key = toBadgeKey(value);
    return `${key} ${BADGE_CHIP_CLASSES[key]}`;
  }
}
