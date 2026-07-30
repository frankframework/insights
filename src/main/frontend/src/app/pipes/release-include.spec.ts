import {
  areRangesIncluded,
  createExactVersionRanges,
  createReleaseLineRanges,
  IncludeRange,
  isVersionIncluded,
  mergeIncludeRanges,
  parseIncludeRanges,
  serializeIncludeRanges,
  WILDCARD_SEGMENT,
} from './release-include';

const range = (
  fromMajor: number,
  fromMinor: number,
  fromPatch: number,
  toMajor: number,
  toMinor: number,
  toPatch: number,
): IncludeRange => ({
  from: { major: fromMajor, minor: fromMinor, patch: fromPatch },
  to: { major: toMajor, minor: toMinor, patch: toPatch },
});

const line = (major: number, minor: number): IncludeRange =>
  range(major, minor, 0, major, minor, WILDCARD_SEGMENT);

const exact = (major: number, minor: number, patch: number): IncludeRange =>
  range(major, minor, patch, major, minor, patch);

describe('parseIncludeRanges', () => {
  it('returns nothing for an absent or empty parameter', () => {
    const queryParameters: Record<string, string> = {};

    expect(parseIncludeRanges(queryParameters['include'])).toEqual([]);
    expect(parseIncludeRanges(null)).toEqual([]);
    expect(parseIncludeRanges('')).toEqual([]);
  });

  it('reads a release line as every patch of that line', () => {
    expect(parseIncludeRanges('7.1')).toEqual([line(7, 1)]);
  });

  it('reads an exact version as that single release', () => {
    expect(parseIncludeRanges('9.3.2')).toEqual([exact(9, 3, 2)]);
  });

  it('ignores a leading v', () => {
    expect(parseIncludeRanges('v9.3.2')).toEqual([exact(9, 3, 2)]);
  });

  it('reads a patch wildcard as the whole release line', () => {
    expect(parseIncludeRanges('9.3.x')).toEqual([line(9, 3)]);
  });

  it('reads a minor wildcard as the whole major', () => {
    expect(parseIncludeRanges('7.x')).toEqual([range(7, 0, 0, 7, WILDCARD_SEGMENT, WILDCARD_SEGMENT)]);
  });

  it('reads a bare major as the whole major', () => {
    expect(parseIncludeRanges('7')).toEqual([range(7, 0, 0, 7, WILDCARD_SEGMENT, WILDCARD_SEGMENT)]);
  });

  it('reads a range between release lines', () => {
    expect(parseIncludeRanges('7.0-10.0')).toEqual([range(7, 0, 0, 10, 0, WILDCARD_SEGMENT)]);
  });

  it('reads a range that ends on an exact patch', () => {
    expect(parseIncludeRanges('7.0-9.3.2')).toEqual([range(7, 0, 0, 9, 3, 2)]);
  });

  it('reads a range that starts on an exact patch', () => {
    expect(parseIncludeRanges('9.3.2-9.5')).toEqual([range(9, 3, 2, 9, 5, WILDCARD_SEGMENT)]);
  });

  it('accepts a slash as range separator', () => {
    expect(parseIncludeRanges('7.0/9.3.2')).toEqual([range(7, 0, 0, 9, 3, 2)]);
  });

  it('accepts dots as range separator', () => {
    expect(parseIncludeRanges('7.0..9.3.2')).toEqual([range(7, 0, 0, 9, 3, 2)]);
  });

  it('reads a list of release lines', () => {
    expect(parseIncludeRanges('8.0,8.1,8.3')).toEqual([line(8, 0), line(8, 1), line(8, 3)]);
  });

  it('keeps touching release lines apart instead of collapsing them', () => {
    expect(serializeIncludeRanges(parseIncludeRanges('8.0,8.1'))).toBe('8.0,8.1');
  });

  it('folds overlapping ranges together', () => {
    expect(parseIncludeRanges('7.0-8.0,7.5-9.0')).toEqual([range(7, 0, 0, 9, 0, WILDCARD_SEGMENT)]);
  });

  it('drops a range that is contained in another', () => {
    expect(parseIncludeRanges('7.0-9.0,8.1.4')).toEqual([range(7, 0, 0, 9, 0, WILDCARD_SEGMENT)]);
  });

  it('removes duplicates', () => {
    expect(parseIncludeRanges('9.3.2,9.3.2')).toEqual([exact(9, 3, 2)]);
  });

  it('sorts entries that arrive out of order', () => {
    expect(parseIncludeRanges('9.3,7.1')).toEqual([line(7, 1), line(9, 3)]);
  });

  it('normalises a reversed range', () => {
    expect(parseIncludeRanges('9.3.2-7.0')).toEqual([range(7, 0, 0, 9, 3, 2)]);
  });

  it('keeps the wildcard bounds of a reversed range', () => {
    expect(parseIncludeRanges('9.x-7.x')).toEqual([range(7, 0, 0, 9, WILDCARD_SEGMENT, WILDCARD_SEGMENT)]);
  });

  it('tolerates whitespace', () => {
    expect(parseIncludeRanges(' 7.0 - 8.0 , 9.1.2 ')).toEqual([range(7, 0, 0, 8, 0, WILDCARD_SEGMENT), exact(9, 1, 2)]);
  });

  it('skips unparsable entries instead of failing', () => {
    expect(parseIncludeRanges('nonsense,7.1,,8.0-')).toEqual([line(7, 1)]);
  });

  it('skips an entry with too many range parts', () => {
    expect(parseIncludeRanges('7.0-8.0-9.0,7.1')).toEqual([line(7, 1)]);
  });
});

describe('serializeIncludeRanges', () => {
  it('returns an empty string for an empty whitelist', () => {
    expect(serializeIncludeRanges([])).toBe('');
  });

  const roundTrips = [
    '7.1',
    '9.3.2',
    '7.x',
    '7.0-10.0',
    '7.0-9.3.2',
    '9.3.2-9.5',
    '8.0,8.1,8.3',
    '7.1,9.3.2',
    '7.0-8.0,9.1.2',
  ];

  for (const parameter of roundTrips) {
    it(`round trips ${parameter}`, () => {
      expect(serializeIncludeRanges(parseIncludeRanges(parameter))).toBe(parameter);
    });
  }

  it('canonicalises a slash separated range to a dash', () => {
    expect(serializeIncludeRanges(parseIncludeRanges('7.0/10.0'))).toBe('7.0-10.0');
  });

  it('canonicalises a bare major to a wildcard', () => {
    expect(serializeIncludeRanges(parseIncludeRanges('7'))).toBe('7.x');
  });

  it('canonicalises a patch wildcard to the release line', () => {
    expect(serializeIncludeRanges(parseIncludeRanges('9.3.x'))).toBe('9.3');
  });

  it('writes a wildcard upper bound spanning majors', () => {
    expect(serializeIncludeRanges(parseIncludeRanges('7.x-10.x'))).toBe('7.0-10.x');
  });

  it('writes a range inside one release line', () => {
    expect(serializeIncludeRanges(parseIncludeRanges('9.3.0-9.3.5'))).toBe('9.3-9.3.5');
  });
});

describe('isVersionIncluded', () => {
  it('is false without a whitelist', () => {
    expect(isVersionIncluded([], 7, 1, 0)).toBeFalse();
  });

  it('matches every patch of an included release line', () => {
    const ranges = parseIncludeRanges('7.1');

    expect(isVersionIncluded(ranges, 7, 1, 0)).toBeTrue();
    expect(isVersionIncluded(ranges, 7, 1, 14)).toBeTrue();
    expect(isVersionIncluded(ranges, 7, 2, 0)).toBeFalse();
  });

  it('matches only the exact release for an exact entry', () => {
    const ranges = parseIncludeRanges('9.3.2');

    expect(isVersionIncluded(ranges, 9, 3, 2)).toBeTrue();
    expect(isVersionIncluded(ranges, 9, 3, 1)).toBeFalse();
    expect(isVersionIncluded(ranges, 9, 3, 3)).toBeFalse();
  });

  it('cuts a release line off at a patch bound', () => {
    const ranges = parseIncludeRanges('7.0-9.3.2');

    expect(isVersionIncluded(ranges, 7, 0, 0)).toBeTrue();
    expect(isVersionIncluded(ranges, 8, 4, 7)).toBeTrue();
    expect(isVersionIncluded(ranges, 9, 3, 2)).toBeTrue();
    expect(isVersionIncluded(ranges, 9, 3, 3)).toBeFalse();
    expect(isVersionIncluded(ranges, 9, 4, 0)).toBeFalse();
  });

  it('skips a release line that is left out of a list', () => {
    const ranges = parseIncludeRanges('8.0,8.1,8.3');

    expect(isVersionIncluded(ranges, 8, 0, 3)).toBeTrue();
    expect(isVersionIncluded(ranges, 8, 1, 0)).toBeTrue();
    expect(isVersionIncluded(ranges, 8, 2, 0)).toBeFalse();
    expect(isVersionIncluded(ranges, 8, 3, 9)).toBeTrue();
  });

  it('matches every release of a wildcard major', () => {
    const ranges = parseIncludeRanges('7.x');

    expect(isVersionIncluded(ranges, 7, 0, 0)).toBeTrue();
    expect(isVersionIncluded(ranges, 7, 12, 5)).toBeTrue();
    expect(isVersionIncluded(ranges, 8, 0, 0)).toBeFalse();
  });
});

describe('mergeIncludeRanges', () => {
  it('adds a range to an empty whitelist', () => {
    expect(serializeIncludeRanges(mergeIncludeRanges([], parseIncludeRanges('7.1')))).toBe('7.1');
  });

  it('keeps a disjunct range separate', () => {
    const merged = mergeIncludeRanges(parseIncludeRanges('8.0'), parseIncludeRanges('8.3'));

    expect(serializeIncludeRanges(merged)).toBe('8.0,8.3');
  });

  it('keeps a touching release line separate', () => {
    const merged = mergeIncludeRanges(parseIncludeRanges('8.0'), parseIncludeRanges('8.1'));

    expect(serializeIncludeRanges(merged)).toBe('8.0,8.1');
  });

  it('is idempotent for a range that is already included', () => {
    const merged = mergeIncludeRanges(parseIncludeRanges('7.0-9.0'), parseIncludeRanges('8.1.4'));

    expect(serializeIncludeRanges(merged)).toBe('7.0-9.0');
  });

  it('extends a range that is partly covered', () => {
    const merged = mergeIncludeRanges(parseIncludeRanges('7.0-9.3.2'), parseIncludeRanges('9.3.1-9.5'));

    expect(serializeIncludeRanges(merged)).toBe('7.0-9.5');
  });
});

describe('createReleaseLineRanges', () => {
  it('covers every patch of the lines of the given releases', () => {
    expect(createReleaseLineRanges(['v7.1.0', 'v7.1.4'])).toEqual([line(7, 1)]);
  });

  it('does not bridge a gap between release lines', () => {
    expect(serializeIncludeRanges(createReleaseLineRanges(['v8.0.0', 'v8.1.0', 'v8.3.0']))).toBe('8.0,8.1,8.3');
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

describe('areRangesIncluded', () => {
  it('is false when the whitelist is empty', () => {
    expect(areRangesIncluded([], [line(7, 1)])).toBeFalse();
  });

  it('is false without candidates', () => {
    expect(areRangesIncluded(parseIncludeRanges('7.1'), [])).toBeFalse();
  });

  it('is true when every candidate is fully covered', () => {
    expect(areRangesIncluded(parseIncludeRanges('7.0-8.0'), [line(7, 1), exact(7, 9, 2)])).toBeTrue();
  });

  it('is false when a candidate reaches past the whitelist', () => {
    expect(areRangesIncluded(parseIncludeRanges('7.0-9.3.2'), [line(9, 3)])).toBeFalse();
  });

  it('is false when one candidate falls outside', () => {
    expect(areRangesIncluded(parseIncludeRanges('7.0-8.0'), [line(7, 1), line(9, 0)])).toBeFalse();
  });
});
