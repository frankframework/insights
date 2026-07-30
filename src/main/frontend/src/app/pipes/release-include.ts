export interface VersionBound {
  major: number;
  minor: number;
  patch: number;
}

export interface IncludeRange {
  from: VersionBound;
  to: VersionBound;
}

export const WILDCARD_SEGMENT = 999;

const MAJOR_KEY_SPACE = 1_000_000;
const MINOR_KEY_SPACE = 1000;
const ENTRY_SEPARATOR_PATTERN = /[,;]/;
const RANGE_SEPARATOR_PATTERN = /\.\.|[-/]/;
const BOUNDARY_PATTERN = /^v?(\d+)(?:\.(\d+|x|\*))?(?:\.(\d+|x|\*))?$/i;
const VERSION_NAME_PATTERN = /^v?(\d+)\.(\d+)(?:\.(\d+))?/i;

interface ParsedVersion {
  major: number;
  minor: number | null;
  patch: number | null;
}

export function parseIncludeRanges(value: string | null | undefined): IncludeRange[] {
  if (!value) return [];

  const ranges = value
    .split(ENTRY_SEPARATOR_PATTERN)
    .map((entry) => parseIncludeEntry(entry))
    .filter((range): range is IncludeRange => range !== null);

  return normaliseIncludeRanges(ranges);
}

export function serializeIncludeRanges(ranges: IncludeRange[]): string {
  return ranges.map((range) => serializeIncludeRange(range)).join(',');
}

export function isVersionIncluded(ranges: IncludeRange[], major: number, minor: number, patch: number): boolean {
  const key = toVersionKey({ major, minor, patch });
  return ranges.some((range) => key >= toVersionKey(range.from) && key <= toVersionKey(range.to));
}

export function mergeIncludeRanges(ranges: IncludeRange[], additions: IncludeRange[]): IncludeRange[] {
  return normaliseIncludeRanges([...ranges, ...additions]);
}

export function createReleaseLineRanges(versions: string[]): IncludeRange[] {
  return buildRanges(versions, (version) => ({
    from: { major: version.major, minor: version.minor ?? 0, patch: 0 },
    to: { major: version.major, minor: version.minor ?? WILDCARD_SEGMENT, patch: WILDCARD_SEGMENT },
  }));
}

export function createExactVersionRanges(versions: string[]): IncludeRange[] {
  return buildRanges(versions, (version) => {
    const bound = { major: version.major, minor: version.minor ?? 0, patch: version.patch ?? 0 };
    return { from: bound, to: { ...bound } };
  });
}

export function areRangesIncluded(ranges: IncludeRange[], candidates: IncludeRange[]): boolean {
  if (ranges.length === 0 || candidates.length === 0) return false;

  return candidates.every((candidate) =>
    ranges.some(
      (range) =>
        toVersionKey(range.from) <= toVersionKey(candidate.from) &&
        toVersionKey(candidate.to) <= toVersionKey(range.to),
    ),
  );
}

function buildRanges(versions: string[], toRange: (version: ParsedVersion) => IncludeRange): IncludeRange[] {
  const ranges = versions
    .map((version) => parseVersionName(version))
    .filter((version): version is ParsedVersion => version !== null)
    .map((version) => toRange(version));

  return normaliseIncludeRanges(ranges);
}

function parseVersionName(version: string): ParsedVersion | null {
  const match = version.trim().match(VERSION_NAME_PATTERN);
  if (!match) return null;

  return {
    major: Number.parseInt(match[1], 10),
    minor: Number.parseInt(match[2], 10),
    patch: match[3] === undefined ? null : Number.parseInt(match[3], 10),
  };
}

function parseIncludeEntry(entry: string): IncludeRange | null {
  const trimmedEntry = entry.trim();
  if (trimmedEntry.length === 0) return null;

  const versions = trimmedEntry.split(RANGE_SEPARATOR_PATTERN).map((part) => parseBoundaryVersion(part));
  if (versions.length > 2 || versions.includes(null)) return null;

  const [first, second] = versions as ParsedVersion[];
  const [start, end] = second === undefined ? [first, first] : orderVersions(first, second);

  return { from: toLowerBound(start), to: toUpperBound(end) };
}

function orderVersions(first: ParsedVersion, second: ParsedVersion): [ParsedVersion, ParsedVersion] {
  return toVersionKey(toLowerBound(first)) <= toVersionKey(toLowerBound(second)) ? [first, second] : [second, first];
}

function parseBoundaryVersion(part: string): ParsedVersion | null {
  const match = part.trim().match(BOUNDARY_PATTERN);
  if (!match) return null;

  return {
    major: Number.parseInt(match[1], 10),
    minor: parseSegment(match[2]),
    patch: parseSegment(match[3]),
  };
}

function parseSegment(segment: string | undefined): number | null {
  if (segment === undefined || segment.toLowerCase() === 'x' || segment === '*') return null;
  return Number.parseInt(segment, 10);
}

function toLowerBound(version: ParsedVersion): VersionBound {
  return { major: version.major, minor: version.minor ?? 0, patch: version.patch ?? 0 };
}

function toUpperBound(version: ParsedVersion): VersionBound {
  return {
    major: version.major,
    minor: version.minor ?? WILDCARD_SEGMENT,
    patch: version.patch ?? WILDCARD_SEGMENT,
  };
}

function normaliseIncludeRanges(ranges: IncludeRange[]): IncludeRange[] {
  const sortedRanges = [...ranges].toSorted((a, b) => toVersionKey(a.from) - toVersionKey(b.from));
  const normalisedRanges: IncludeRange[] = [];

  for (const range of sortedRanges) {
    const previousRange = normalisedRanges.at(-1);

    if (previousRange && toVersionKey(range.from) <= toVersionKey(previousRange.to)) {
      if (toVersionKey(range.to) > toVersionKey(previousRange.to)) {
        previousRange.to = { ...range.to };
      }
      continue;
    }

    normalisedRanges.push({ from: { ...range.from }, to: { ...range.to } });
  }

  return normalisedRanges;
}

function serializeIncludeRange(range: IncludeRange): string {
  const singleVersion = serializeAsSingleVersion(range);
  if (singleVersion) return singleVersion;

  return `${formatLowerBound(range.from)}-${formatUpperBound(range.to)}`;
}

function serializeAsSingleVersion(range: IncludeRange): string | null {
  const { from, to } = range;
  if (from.major !== to.major) return null;

  if (from.minor === 0 && from.patch === 0 && to.minor === WILDCARD_SEGMENT && to.patch === WILDCARD_SEGMENT) {
    return `${from.major}.x`;
  }

  if (from.minor === to.minor && from.patch === 0 && to.patch === WILDCARD_SEGMENT) {
    return `${from.major}.${from.minor}`;
  }

  if (from.minor === to.minor && from.patch === to.patch) {
    return `${from.major}.${from.minor}.${from.patch}`;
  }

  return null;
}

function formatLowerBound(bound: VersionBound): string {
  if (bound.patch === 0) {
    return `${bound.major}.${bound.minor}`;
  }
  return `${bound.major}.${bound.minor}.${bound.patch}`;
}

function formatUpperBound(bound: VersionBound): string {
  if (bound.minor === WILDCARD_SEGMENT && bound.patch === WILDCARD_SEGMENT) {
    return `${bound.major}.x`;
  }
  if (bound.patch === WILDCARD_SEGMENT) {
    return `${bound.major}.${bound.minor}`;
  }
  return `${bound.major}.${bound.minor}.${bound.patch}`;
}

function toVersionKey(bound: VersionBound): number {
  return bound.major * MAJOR_KEY_SPACE + bound.minor * MINOR_KEY_SPACE + bound.patch;
}
