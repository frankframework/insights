import {
  areRangesCovered,
  createExactVersionRanges,
  createLineRange,
  createOpenEndedRange,
  createReleaseLineRanges,
  isVersionInRanges,
  MAX_SEGMENT,
  mergeVersionRanges,
  parseVersionRanges,
  serializeVersionRanges,
  VersionRange,
} from './release-range';

const range = (
  fromMajor: number,
  fromMinor: number,
  fromPatch: number,
  toMajor: number,
  toMinor: number,
  toPatch: number,
): VersionRange => ({
  from: { major: fromMajor, minor: fromMinor, patch: fromPatch },
  to: { major: toMajor, minor: toMinor, patch: toPatch },
});

const line = (major: number, minor: number): VersionRange => range(major, minor, 0, major, minor, MAX_SEGMENT);

const exact = (major: number, minor: number, patch: number): VersionRange =>
  range(major, minor, patch, major, minor, patch);

const rangesOf = (specification: string): VersionRange[] => parseVersionRanges(specification).ranges;

const errorOf = (specification: string): string | null => parseVersionRanges(specification).error;

/**
 * Every row of https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html, checked against
 * the releases the notation is meant to select.
 */
describe('the Maven enforcer version range rules', () => {
  const releases = ['8.9.9', '9.0.0', '9.0.1', '9.0.5', '9.1.0', '9.1.1', '9.2.0', '10.0.0', '10.0.1'];

  const selects = (specification: string): string[] =>
    releases.filter((release) => {
      const [major, minor, patch] = release.split('.').map(Number);
      return isVersionInRanges(rangesOf(specification), major, minor, patch);
    });

  it('reads 9.0 as x >= 9.0', () => {
    expect(selects('9.0')).toEqual(['9.0.0', '9.0.1', '9.0.5', '9.1.0', '9.1.1', '9.2.0', '10.0.0', '10.0.1']);
  });

  it('reads (,9.0] as x <= 9.0', () => {
    expect(selects('(,9.0]')).toEqual(['8.9.9', '9.0.0']);
  });

  it('reads (,9.0) as x < 9.0', () => {
    expect(selects('(,9.0)')).toEqual(['8.9.9']);
  });

  it('reads [9.0] as x == 9.0', () => {
    expect(selects('[9.0]')).toEqual(['9.0.0']);
  });

  it('reads [9.0,) as x >= 9.0', () => {
    expect(selects('[9.0,)')).toEqual(['9.0.0', '9.0.1', '9.0.5', '9.1.0', '9.1.1', '9.2.0', '10.0.0', '10.0.1']);
  });

  it('reads (9.0,) as x > 9.0', () => {
    expect(selects('(9.0,)')).toEqual(['9.0.1', '9.0.5', '9.1.0', '9.1.1', '9.2.0', '10.0.0', '10.0.1']);
  });

  it('reads (9.0,10.0) as 9.0 < x < 10.0', () => {
    expect(selects('(9.0,10.0)')).toEqual(['9.0.1', '9.0.5', '9.1.0', '9.1.1', '9.2.0']);
  });

  it('reads [9.0,10.0] as 9.0 <= x <= 10.0', () => {
    expect(selects('[9.0,10.0]')).toEqual(['9.0.0', '9.0.1', '9.0.5', '9.1.0', '9.1.1', '9.2.0', '10.0.0']);
  });

  it('reads (,9.0],[9.2,) as x <= 9.0 or x >= 9.2, comma separating the sets', () => {
    expect(selects('(,9.0],[9.2,)')).toEqual(['8.9.9', '9.0.0', '9.2.0', '10.0.0', '10.0.1']);
  });

  it('reads (,9.1),(9.1,) as x != 9.1', () => {
    expect(selects('(,9.1),(9.1,)')).toEqual([
      '8.9.9',
      '9.0.0',
      '9.0.1',
      '9.0.5',
      '9.1.1',
      '9.2.0',
      '10.0.0',
      '10.0.1',
    ]);
  });

  it('equates 9, 9.0 and 9.0.0 the way Maven does', () => {
    expect(selects('[9]')).toEqual(['9.0.0']);
    expect(selects('[9.0]')).toEqual(['9.0.0']);
    expect(selects('[9.0.0]')).toEqual(['9.0.0']);
  });

  it('writes a whole patch line as the half open range Maven uses for it', () => {
    expect(selects('[9.0,9.1)')).toEqual(['9.0.0', '9.0.1', '9.0.5']);
  });
});

describe('parseVersionRanges', () => {
  it('returns nothing for an absent or empty parameter', () => {
    const queryParameters: Record<string, string> = {};

    expect(parseVersionRanges(queryParameters['range'])).toEqual({ ranges: [], error: null });
    expect(parseVersionRanges(null)).toEqual({ ranges: [], error: null });
    expect(parseVersionRanges('   ')).toEqual({ ranges: [], error: null });
  });

  it('reads a pinned version as that single release', () => {
    expect(rangesOf('[9.0]')).toEqual([exact(9, 0, 0)]);
  });

  it('reads a pinned exact version as that single release', () => {
    expect(rangesOf('[9.3.2]')).toEqual([exact(9, 3, 2)]);
  });

  it('reads a pinned major as the zero release of that major', () => {
    expect(rangesOf('[9]')).toEqual([exact(9, 0, 0)]);
  });

  it('ignores a leading v', () => {
    expect(rangesOf('[v9.3.2]')).toEqual([exact(9, 3, 2)]);
  });

  it('reads an open ended range as everything from that release on', () => {
    expect(rangesOf('[10.0,)')).toEqual([{ from: { major: 10, minor: 0, patch: 0 }, to: null }]);
  });

  it('reads an open beginning as everything up to that release', () => {
    expect(rangesOf('(,9.4]')).toEqual([{ from: null, to: { major: 9, minor: 4, patch: 0 } }]);
  });

  it('reads a bare version as a minimum version', () => {
    expect(rangesOf('9.0')).toEqual([{ from: { major: 9, minor: 0, patch: 0 }, to: null }]);
  });

  it('keeps an inclusive upper bound on the release it names', () => {
    expect(rangesOf('[9.0,9.4]')).toEqual([range(9, 0, 0, 9, 4, 0)]);
  });

  it('stops an exclusive upper bound one release before the one it names', () => {
    expect(rangesOf('[9.0,9.4)')).toEqual([range(9, 0, 0, 9, 3, MAX_SEGMENT)]);
  });

  it('starts an exclusive lower bound one release after the one it names', () => {
    expect(rangesOf('(9.4,)')).toEqual([{ from: { major: 9, minor: 4, patch: 1 }, to: null }]);
  });

  it('reads a release line written as a half open range', () => {
    expect(rangesOf('[9.0,9.1)')).toEqual([line(9, 0)]);
  });

  it('reads a list of ranges without splitting the commas inside them', () => {
    expect(rangesOf('[9.0],[9.4],[10.0,)')).toEqual([
      exact(9, 0, 0),
      exact(9, 4, 0),
      { from: { major: 10, minor: 0, patch: 0 }, to: null },
    ]);
  });

  it('tolerates whitespace around and inside entries', () => {
    expect(rangesOf(' [9.0] , [ 10.0 , ) ')).toEqual([
      exact(9, 0, 0),
      { from: { major: 10, minor: 0, patch: 0 }, to: null },
    ]);
  });

  it('sorts entries that arrive out of order', () => {
    expect(rangesOf('[9.3],[7.1]')).toEqual([exact(7, 1, 0), exact(9, 3, 0)]);
  });

  it('folds overlapping ranges together', () => {
    expect(rangesOf('[7.0,8.0],[7.5,9.0]')).toEqual([range(7, 0, 0, 9, 0, 0)]);
  });

  it('folds ranges that touch at the next release together', () => {
    expect(rangesOf('[8.0],[8.0.1]')).toEqual([range(8, 0, 0, 8, 0, 1)]);
  });

  it('folds neighbouring release lines together', () => {
    expect(rangesOf('[8.0,8.1),[8.1,8.2)')).toEqual([range(8, 0, 0, 8, 1, MAX_SEGMENT)]);
  });

  it('keeps releases with a gap between them apart', () => {
    expect(rangesOf('[8.0],[8.3]')).toEqual([exact(8, 0, 0), exact(8, 3, 0)]);
  });

  it('drops a range that is contained in another', () => {
    expect(rangesOf('[7.0,9.0],[8.1.4]')).toEqual([range(7, 0, 0, 9, 0, 0)]);
  });

  it('lets an open ended range swallow the ranges after it', () => {
    expect(rangesOf('[9.0,),[10.0]')).toEqual([{ from: { major: 9, minor: 0, patch: 0 }, to: null }]);
  });

  it('removes duplicates', () => {
    expect(rangesOf('[9.3.2],[9.3.2]')).toEqual([exact(9, 3, 2)]);
  });
});

describe('parseVersionRanges errors', () => {
  it('reports a missing closing bracket', () => {
    expect(errorOf('[9.0')).toBe('"[9.0" is not a valid version range: it is missing a closing bracket.');
  });

  it('reports a missing opening bracket', () => {
    expect(errorOf('9.0]')).toBe('"9.0]" is not a valid version range: it closes a bracket that was never opened.');
  });

  it('reports a version that is not a number', () => {
    expect(errorOf('[9.x]')).toBe('"[9.x]" is not a valid version range: "9.x" is not a version number.');
  });

  it('reports a bare entry that is not a version', () => {
    expect(errorOf('nonsense')).toBe('"nonsense" is not a valid version range: "nonsense" is not a version number.');
  });

  it('reports a single version in round brackets', () => {
    expect(errorOf('(9.0)')).toBe(
      '"(9.0)" is not a valid version range: a single version needs square brackets, like [9.0].',
    );
  });

  it('reports an entry with more than two versions', () => {
    expect(errorOf('[7.0,8.0,9.0]')).toBe('"[7.0,8.0,9.0]" is not a valid version range: it holds more than two versions.');
  });

  it('reports a lower bound above the upper bound', () => {
    expect(errorOf('[9.0,7.0]')).toBe('"[9.0,7.0]" is not a valid version range: its lower bound is above its upper bound.');
  });

  it('reports an empty entry', () => {
    expect(errorOf('[9.0],,[9.4]')).toBe('"[9.0],,[9.4]" is not a valid version range: it contains an empty entry.');
  });

  it('reports a trailing comma', () => {
    expect(errorOf('[9.0],')).toBe('"[9.0]," is not a valid version range: it contains an empty entry.');
  });

  it('returns no ranges at all when a single entry is broken', () => {
    expect(rangesOf('[9.0],[nonsense]')).toEqual([]);
  });

  it('accepts every valid entry without an error', () => {
    expect(errorOf('[9.0],[9.4],[10.0,)')).toBeNull();
  });
});

describe('serializeVersionRanges', () => {
  it('returns an empty string without ranges', () => {
    expect(serializeVersionRanges([])).toBe('');
  });

  const roundTrips = [
    '[9.0]',
    '[9.3.2]',
    '[10.0,)',
    '(,9.4]',
    '(,9.4)',
    '[9.0,9.4]',
    '[9.0,9.4)',
    '[9.0,9.1)',
    '[7.0,9.3.2]',
    '[9.0],[9.4],[10.0,)',
    '[7.1],[9.3.2]',
    '(,)',
  ];

  for (const parameter of roundTrips) {
    it(`round trips ${parameter}`, () => {
      expect(serializeVersionRanges(rangesOf(parameter))).toBe(parameter);
    });
  }

  it('canonicalises a bare version to an open ended range', () => {
    expect(serializeVersionRanges(rangesOf('9.0'))).toBe('[9.0,)');
  });

  it('canonicalises a pinned major to the release it means', () => {
    expect(serializeVersionRanges(rangesOf('[9]'))).toBe('[9.0]');
  });

  it('canonicalises an exclusive lower bound to the release after it', () => {
    expect(serializeVersionRanges(rangesOf('(9.4,)'))).toBe('[9.4.1,)');
  });

  it('writes a bound on the last patch of a line as an exclusive bound on the next line', () => {
    expect(serializeVersionRanges([line(9, 0)])).toBe('[9.0,9.1)');
  });

  it('writes a whole major as an exclusive bound on the next major', () => {
    expect(serializeVersionRanges([range(9, 0, 0, 9, MAX_SEGMENT, MAX_SEGMENT)])).toBe('[9.0,10.0)');
  });
});

describe('isVersionInRanges', () => {
  it('places no restriction without ranges', () => {
    expect(isVersionInRanges([], 7, 1, 0)).toBeTrue();
  });

  it('matches only the named release for a pinned version', () => {
    const ranges = rangesOf('[7.1]');

    expect(isVersionInRanges(ranges, 7, 1, 0)).toBeTrue();
    expect(isVersionInRanges(ranges, 7, 1, 14)).toBeFalse();
    expect(isVersionInRanges(ranges, 7, 2, 0)).toBeFalse();
  });

  it('matches every patch of a release line written as a half open range', () => {
    const ranges = rangesOf('[7.1,7.2)');

    expect(isVersionInRanges(ranges, 7, 1, 0)).toBeTrue();
    expect(isVersionInRanges(ranges, 7, 1, 14)).toBeTrue();
    expect(isVersionInRanges(ranges, 7, 2, 0)).toBeFalse();
  });

  it('matches only the exact release for a pinned version', () => {
    const ranges = rangesOf('[9.3.2]');

    expect(isVersionInRanges(ranges, 9, 3, 2)).toBeTrue();
    expect(isVersionInRanges(ranges, 9, 3, 1)).toBeFalse();
    expect(isVersionInRanges(ranges, 9, 3, 3)).toBeFalse();
  });

  it('matches everything from an open ended range on', () => {
    const ranges = rangesOf('[10.0,)');

    expect(isVersionInRanges(ranges, 9, 4, 9)).toBeFalse();
    expect(isVersionInRanges(ranges, 10, 0, 0)).toBeTrue();
    expect(isVersionInRanges(ranges, 12, 3, 4)).toBeTrue();
  });

  it('matches everything up to an open beginning', () => {
    const ranges = rangesOf('(,9.4]');

    expect(isVersionInRanges(ranges, 1, 0, 0)).toBeTrue();
    expect(isVersionInRanges(ranges, 9, 4, 0)).toBeTrue();
    expect(isVersionInRanges(ranges, 9, 4, 7)).toBeFalse();
  });

  it('cuts a release line off at a patch bound', () => {
    const ranges = rangesOf('[7.0,9.3.2]');

    expect(isVersionInRanges(ranges, 7, 0, 0)).toBeTrue();
    expect(isVersionInRanges(ranges, 8, 4, 7)).toBeTrue();
    expect(isVersionInRanges(ranges, 9, 3, 2)).toBeTrue();
    expect(isVersionInRanges(ranges, 9, 3, 3)).toBeFalse();
  });

  it('skips a release line that is left out of a list', () => {
    const ranges = rangesOf('[8.0,8.1),[8.3,8.4)');

    expect(isVersionInRanges(ranges, 8, 0, 3)).toBeTrue();
    expect(isVersionInRanges(ranges, 8, 2, 0)).toBeFalse();
    expect(isVersionInRanges(ranges, 8, 3, 9)).toBeTrue();
  });
});

describe('mergeVersionRanges', () => {
  it('adds a range to an empty list', () => {
    expect(serializeVersionRanges(mergeVersionRanges([], rangesOf('[7.1]')))).toBe('[7.1]');
  });

  it('keeps a range with a gap before it separate', () => {
    const merged = mergeVersionRanges(rangesOf('[8.0]'), rangesOf('[8.3]'));

    expect(serializeVersionRanges(merged)).toBe('[8.0],[8.3]');
  });

  it('is idempotent for a range that is already covered', () => {
    const merged = mergeVersionRanges(rangesOf('[7.0,9.0]'), rangesOf('[8.1.4]'));

    expect(serializeVersionRanges(merged)).toBe('[7.0,9.0]');
  });

  it('widens a range that is partly covered', () => {
    const merged = mergeVersionRanges(rangesOf('[7.0,9.3.2]'), rangesOf('[9.3,9.4)'));

    expect(serializeVersionRanges(merged)).toBe('[7.0,9.4)');
  });

  it('adds a skipped release line to the default range', () => {
    const merged = mergeVersionRanges(rangesOf('[9.0],[9.4],[10.0,)'), rangesOf('[8.1]'));

    expect(serializeVersionRanges(merged)).toBe('[8.1],[9.0],[9.4],[10.0,)');
  });
});

describe('createReleaseLineRanges', () => {
  it('covers every patch of the lines of the given releases', () => {
    expect(createReleaseLineRanges(['v7.1.0', 'v7.1.4'])).toEqual([line(7, 1)]);
  });

  it('writes the lines it covers as half open Maven ranges', () => {
    expect(serializeVersionRanges(createReleaseLineRanges(['v8.0.0', 'v8.1.0', 'v8.3.0']))).toBe('[8.0,8.2),[8.3,8.4)');
  });

  it('ignores names without a version', () => {
    expect(createReleaseLineRanges(['master'])).toEqual([]);
  });
});

describe('createExactVersionRanges', () => {
  it('covers only the given release', () => {
    expect(createExactVersionRanges(['v9.3.2'])).toEqual([exact(9, 3, 2)]);
  });

  it('treats a missing patch as patch zero', () => {
    expect(createExactVersionRanges(['v9.3'])).toEqual([exact(9, 3, 0)]);
  });

  it('ignores names that hold no version at all', () => {
    expect(createExactVersionRanges(['nightly'])).toEqual([]);
  });
});

describe('createLineRange and createOpenEndedRange', () => {
  it('writes a release line', () => {
    expect(serializeVersionRanges([createLineRange(9, 4)])).toBe('[9.4,9.5)');
  });

  it('writes an open ended range', () => {
    expect(serializeVersionRanges([createOpenEndedRange(10, 0)])).toBe('[10.0,)');
  });
});

describe('areRangesCovered', () => {
  it('is false without candidates', () => {
    expect(areRangesCovered(rangesOf('[7.1]'), [])).toBeFalse();
  });

  it('is true when nothing restricts the graph', () => {
    expect(areRangesCovered([], [line(7, 1)])).toBeTrue();
  });

  it('is true when every candidate is fully covered', () => {
    expect(areRangesCovered(rangesOf('[7.0,8.0]'), [line(7, 1), exact(7, 9, 2)])).toBeTrue();
  });

  it('is true when an open ended range covers the candidate', () => {
    expect(areRangesCovered(rangesOf('[10.0,)'), [line(12, 4)])).toBeTrue();
  });

  it('is false when a candidate reaches past the ranges', () => {
    expect(areRangesCovered(rangesOf('[7.0,9.3.2]'), [line(9, 3)])).toBeFalse();
  });

  it('is false when one candidate falls outside', () => {
    expect(areRangesCovered(rangesOf('[7.0,8.0]'), [line(7, 1), line(9, 0)])).toBeFalse();
  });
});
