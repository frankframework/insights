export function parseVersion(s: string): number[] {
  return s
    .replace(/^[^0-9]*/, '')
    .split('.')
    .map((n) => Number.parseInt(n, 10) || 0);
}

export function compareVersions(a: string, b: string): number {
  const pa = parseVersion(a);
  const pb = parseVersion(b);
  for (let index = 0; index < Math.max(pa.length, pb.length); index++) {
    const diff = (pa[index] ?? 0) - (pb[index] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

export function sortVersionsAsc(versions: string[]): string[] {
  return [...versions].toSorted(compareVersions);
}
