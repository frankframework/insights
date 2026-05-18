export function parseVersion(version: string): number[] {
  return version
    .replace(/^[^0-9]*/, '')
    .split('.')
    .map((versionNumber) => Number.parseInt(versionNumber, 10) || 0);
}

export function compareVersions(a: string, b: string): number {
  const parseVersionA = parseVersion(a);
  const parseVersionB = parseVersion(b);
  for (let index = 0; index < Math.max(parseVersionA.length, parseVersionB.length); index++) {
    const diff = (parseVersionA[index] ?? 0) - (parseVersionB[index] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

export function sortVersionsAsc(versions: string[]): string[] {
  return [...versions].toSorted(compareVersions);
}
