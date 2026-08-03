/**
 * Version ranges for the release graph, written in the Maven range syntax described in
 * https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
 */

export interface VersionBound {
  major: number;
  minor: number;
  patch: number;
}

export interface VersionRange {
  from: VersionBound | null;
  to: VersionBound | null;
}

export interface ParsedVersionRanges {
  ranges: VersionRange[];
  error: string | null;
}

export const MAX_SEGMENT = 99_999;

const SEGMENT_SPACE = MAX_SEGMENT + 1;
const MINOR_KEY_SPACE = SEGMENT_SPACE;
const MAJOR_KEY_SPACE = SEGMENT_SPACE * SEGMENT_SPACE;
const OPENING_BRACKETS = new Set(['[', '(']);
const CLOSING_BRACKETS = new Set([']', ')']);
const VERSION_TOKEN_PATTERN = /^v?(\d+)(?:\.(\d+))?(?:\.(\d+))?$/i;
const VERSION_NAME_PATTERN = /^v?(\d+)\.(\d+)(?:\.(\d+))?/i;

interface ParseFailure {
  error: string;
}

interface VersionToken {
  major: number;
  minor: number | null;
  patch: number | null;
}

type EntryOutcome = VersionRange | ParseFailure;

export function parseVersionRanges(value: string | null | undefined): ParsedVersionRanges {
  const specification = value?.trim() ?? '';
  if (specification.length === 0) return { ranges: [], error: null };

  const entries = splitEntries(specification);
  if (isFailure(entries)) return { ranges: [], error: entries.error };

  const ranges: VersionRange[] = [];
  for (const entry of entries) {
    const outcome = parseEntry(entry);
    if (isFailure(outcome)) return { ranges: [], error: outcome.error };
    ranges.push(outcome);
  }

  return { ranges: normaliseRanges(ranges), error: null };
}

export function serializeVersionRanges(ranges: VersionRange[]): string {
  return ranges.map((range) => serializeRange(range)).join(',');
}

export function isVersionInRanges(ranges: VersionRange[], major: number, minor: number, patch: number): boolean {
  if (ranges.length === 0) return true;

  const key = toKey({ major, minor, patch });
  return ranges.some((range) => lowerKeyOf(range) <= key && key <= upperKeyOf(range));
}

export function mergeVersionRanges(ranges: VersionRange[], additions: VersionRange[]): VersionRange[] {
  return normaliseRanges([...ranges, ...additions]);
}

export function areRangesCovered(ranges: VersionRange[], candidates: VersionRange[]): boolean {
  if (candidates.length === 0) return false;
  if (ranges.length === 0) return true;

  return candidates.every((candidate) =>
    ranges.some((range) => lowerKeyOf(range) <= lowerKeyOf(candidate) && upperKeyOf(candidate) <= upperKeyOf(range)),
  );
}

export function createLineRange(major: number, minor: number): VersionRange {
  return { from: { major, minor, patch: 0 }, to: { major, minor, patch: MAX_SEGMENT } };
}

export function createOpenEndedRange(major: number, minor: number): VersionRange {
  return { from: { major, minor, patch: 0 }, to: null };
}

export function createReleaseLineRanges(versions: string[]): VersionRange[] {
  return buildRanges(versions, (version) => createLineRange(version.major, version.minor ?? 0));
}

export function createExactVersionRanges(versions: string[]): VersionRange[] {
  return buildRanges(versions, (version) => {
    const bound = { major: version.major, minor: version.minor ?? 0, patch: version.patch ?? 0 };
    return { from: bound, to: { ...bound } };
  });
}

function buildRanges(versions: string[], toRange: (version: VersionToken) => VersionRange): VersionRange[] {
  const ranges = versions
    .map((version) => parseVersionName(version))
    .filter((version): version is VersionToken => version !== null)
    .map((version) => toRange(version));

  return normaliseRanges(ranges);
}

function parseVersionName(version: string): VersionToken | null {
  const match = version.trim().match(VERSION_NAME_PATTERN);
  if (!match) return null;

  return {
    major: Number.parseInt(match[1], 10),
    minor: Number.parseInt(match[2], 10),
    patch: match[3] === undefined ? null : Number.parseInt(match[3], 10),
  };
}

function splitEntries(specification: string): string[] | ParseFailure {
  const entries: string[] = [];
  let current = '';
  let isInsideBrackets = false;

  for (const character of specification) {
    if (OPENING_BRACKETS.has(character)) {
      if (isInsideBrackets) return failure(specification, 'it opens a second bracket before closing the first');
      isInsideBrackets = true;
    } else if (CLOSING_BRACKETS.has(character)) {
      if (!isInsideBrackets) return failure(specification, 'it closes a bracket that was never opened');
      isInsideBrackets = false;
    } else if (character === ',' && !isInsideBrackets) {
      entries.push(current);
      current = '';
      continue;
    }
    current += character;
  }

  if (isInsideBrackets) return failure(specification, 'it is missing a closing bracket');
  entries.push(current);

  if (entries.some((entry) => entry.trim().length === 0)) {
    return failure(specification, 'it contains an empty entry');
  }

  return entries;
}

function parseEntry(entry: string): EntryOutcome {
  const trimmed = entry.trim();
  const hasOpeningBracket = OPENING_BRACKETS.has(trimmed[0]);
  const hasClosingBracket = CLOSING_BRACKETS.has(trimmed.at(-1)!);

  if (!hasOpeningBracket && !hasClosingBracket) return parseSoftRequirement(trimmed);
  if (!hasOpeningBracket) return failure(trimmed, 'it is missing an opening bracket');
  if (!hasClosingBracket) return failure(trimmed, 'it is missing a closing bracket');

  const isLowerInclusive = trimmed[0] === '[';
  const isUpperInclusive = trimmed.at(-1) === ']';
  const parts = trimmed.slice(1, -1).split(',');

  if (parts.length > 2) return failure(trimmed, 'it holds more than two versions');
  if (parts.length === 1) return parseSingleVersion(trimmed, parts[0], isLowerInclusive, isUpperInclusive);

  return parseBoundedRange(trimmed, parts[0], parts[1], isLowerInclusive, isUpperInclusive);
}

function parseSoftRequirement(entry: string): EntryOutcome {
  const version = parseVersionToken(entry);
  if (!version) return failure(entry, `"${entry}" is not a version number`);

  return { from: toBound(lowerKeyOfToken(version)), to: null };
}

function parseSingleVersion(
  entry: string,
  text: string,
  isLowerInclusive: boolean,
  isUpperInclusive: boolean,
): EntryOutcome {
  if (!isLowerInclusive || !isUpperInclusive) {
    return failure(entry, 'a single version needs square brackets, like [9.0]');
  }

  const version = parseVersionToken(text);
  if (!version) return failure(entry, `"${text.trim()}" is not a version number`);

  return { from: toBound(lowerKeyOfToken(version)), to: toBound(upperKeyOfToken(version)) };
}

function parseBoundedRange(
  entry: string,
  lowerText: string,
  upperText: string,
  isLowerInclusive: boolean,
  isUpperInclusive: boolean,
): EntryOutcome {
  const lower = parseBoundary(entry, lowerText, isLowerInclusive, 'lower');
  if (isFailure(lower)) return lower;

  const upper = parseBoundary(entry, upperText, isUpperInclusive, 'upper');
  if (isFailure(upper)) return upper;

  const range = { from: lower.bound, to: upper.bound };
  if (lowerKeyOf(range) > upperKeyOf(range)) return failure(entry, 'its lower bound is above its upper bound');

  return range;
}

function parseBoundary(
  entry: string,
  text: string,
  isInclusive: boolean,
  side: 'lower' | 'upper',
): { bound: VersionBound | null } | ParseFailure {
  const trimmed = text.trim();
  if (trimmed.length === 0) return { bound: null };

  const version = parseVersionToken(trimmed);
  if (!version) return failure(entry, `"${trimmed}" is not a version number`);

  const key = toBoundaryKey(version, isInclusive, side);
  if (key < 0) return failure(entry, 'it holds no version at all');

  return { bound: toBound(key) };
}

function toBoundaryKey(version: VersionToken, isInclusive: boolean, side: 'lower' | 'upper'): number {
  if (side === 'lower') {
    return isInclusive ? lowerKeyOfToken(version) : upperKeyOfToken(version) + 1;
  }
  return isInclusive ? upperKeyOfToken(version) : lowerKeyOfToken(version) - 1;
}

function parseVersionToken(text: string): VersionToken | null {
  const match = text.trim().match(VERSION_TOKEN_PATTERN);
  if (!match) return null;

  const token = {
    major: Number.parseInt(match[1], 10),
    minor: match[2] === undefined ? null : Number.parseInt(match[2], 10),
    patch: match[3] === undefined ? null : Number.parseInt(match[3], 10),
  };

  const isWithinRange = [token.major, token.minor, token.patch].every(
    (segment) => segment === null || segment <= MAX_SEGMENT,
  );

  return isWithinRange ? token : null;
}

function normaliseRanges(ranges: VersionRange[]): VersionRange[] {
  const sortedRanges = [...ranges].toSorted((a, b) => lowerKeyOf(a) - lowerKeyOf(b));
  const normalisedRanges: VersionRange[] = [];

  for (const range of sortedRanges) {
    const previousRange = normalisedRanges.at(-1);

    if (previousRange && lowerKeyOf(range) <= upperKeyOf(previousRange) + 1) {
      if (upperKeyOf(range) > upperKeyOf(previousRange)) {
        previousRange.to = range.to === null ? null : { ...range.to };
      }
      continue;
    }

    normalisedRanges.push({
      from: range.from === null ? null : { ...range.from },
      to: range.to === null ? null : { ...range.to },
    });
  }

  return normalisedRanges;
}

function serializeRange(range: VersionRange): string {
  if (range.from === null && range.to === null) return '(,)';
  if (range.from === null) return `(,${formatUpperBound(range.to!)}]`;
  if (range.to === null) return `[${formatLowerBound(range.from)},)`;

  const singleVersion = toSingleVersionToken(range.from, range.to);
  if (singleVersion) return `[${singleVersion}]`;

  return `[${formatLowerBound(range.from)},${formatUpperBound(range.to)}]`;
}

function toSingleVersionToken(from: VersionBound, to: VersionBound): string | null {
  if (from.major !== to.major) return null;

  const isWholeMajor = from.minor === 0 && from.patch === 0 && to.minor === MAX_SEGMENT && to.patch === MAX_SEGMENT;
  if (isWholeMajor) return `${from.major}`;

  if (from.minor !== to.minor) return null;
  if (from.patch === 0 && to.patch === MAX_SEGMENT) return `${from.major}.${from.minor}`;

  return from.patch === to.patch ? `${from.major}.${from.minor}.${from.patch}` : null;
}

function formatLowerBound(bound: VersionBound): string {
  return bound.patch === 0 ? `${bound.major}.${bound.minor}` : `${bound.major}.${bound.minor}.${bound.patch}`;
}

function formatUpperBound(bound: VersionBound): string {
  if (bound.minor === MAX_SEGMENT && bound.patch === MAX_SEGMENT) return `${bound.major}`;
  return bound.patch === MAX_SEGMENT ? `${bound.major}.${bound.minor}` : `${bound.major}.${bound.minor}.${bound.patch}`;
}

function lowerKeyOf(range: VersionRange): number {
  return range.from === null ? Number.NEGATIVE_INFINITY : toKey(range.from);
}

function upperKeyOf(range: VersionRange): number {
  return range.to === null ? Number.POSITIVE_INFINITY : toKey(range.to);
}

function lowerKeyOfToken(token: VersionToken): number {
  return toKey({ major: token.major, minor: token.minor ?? 0, patch: token.patch ?? 0 });
}

function upperKeyOfToken(token: VersionToken): number {
  return toKey({
    major: token.major,
    minor: token.minor ?? MAX_SEGMENT,
    patch: token.patch ?? MAX_SEGMENT,
  });
}

function toKey(bound: VersionBound): number {
  return bound.major * MAJOR_KEY_SPACE + bound.minor * MINOR_KEY_SPACE + bound.patch;
}

function toBound(key: number): VersionBound {
  return {
    major: Math.floor(key / MAJOR_KEY_SPACE),
    minor: Math.floor((key % MAJOR_KEY_SPACE) / MINOR_KEY_SPACE),
    patch: key % MINOR_KEY_SPACE,
  };
}

function failure(entry: string, reason: string): ParseFailure {
  return { error: `"${entry}" is not a valid version range: ${reason}.` };
}

function isFailure(outcome: EntryOutcome | string[] | { bound: VersionBound | null }): outcome is ParseFailure {
  return 'error' in outcome;
}
