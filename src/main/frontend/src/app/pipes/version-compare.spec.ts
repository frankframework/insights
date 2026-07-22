import { parseVersion, compareVersions, sortVersionsAsc, sortVersionsDesc } from './version-compare';

describe('parseVersion', () => {
  it('parses simple major.minor', () => {
    expect(parseVersion('7.8')).toEqual([7, 8]);
  });

  it('parses major.minor.patch', () => {
    expect(parseVersion('7.8.2')).toEqual([7, 8, 2]);
  });

  it('strips leading v prefix', () => {
    expect(parseVersion('v9.0.1')).toEqual([9, 0, 1]);
  });

  it('strips leading non-numeric characters', () => {
    expect(parseVersion('release/8.3')).toEqual([8, 3]);
  });

  it('returns [0] for fully non-numeric string', () => {
    const result = parseVersion('master');

    expect(result[0]).toBe(0);
  });

  it('handles single number', () => {
    expect(parseVersion('9')).toEqual([9]);
  });
});

describe('compareVersions', () => {
  it('returns negative when a < b (major)', () => {
    expect(compareVersions('7.0', '8.0')).toBeLessThan(0);
  });

  it('returns positive when a > b (major)', () => {
    expect(compareVersions('9.0', '8.0')).toBeGreaterThan(0);
  });

  it('returns 0 for equal versions', () => {
    expect(compareVersions('8.1', '8.1')).toBe(0);
  });

  it('compares minor versions correctly', () => {
    expect(compareVersions('7.7', '7.8')).toBeLessThan(0);
    expect(compareVersions('7.8', '7.7')).toBeGreaterThan(0);
  });

  it('compares patch versions correctly', () => {
    expect(compareVersions('7.8.1', '7.8.2')).toBeLessThan(0);
    expect(compareVersions('7.8.3', '7.8.2')).toBeGreaterThan(0);
  });

  it('shorter version treated as having 0 for missing segments', () => {
    expect(compareVersions('7.8', '7.8.0')).toBe(0);
  });

  it('cross-branch comparison: 9.0 > 8.3', () => {
    expect(compareVersions('9.0', '8.3')).toBeGreaterThan(0);
  });
});

describe('sortVersionsAsc', () => {
  it('sorts a simple list ascending', () => {
    expect(sortVersionsAsc(['9.0', '7.8', '8.1'])).toEqual(['7.8', '8.1', '9.0']);
  });

  it('sorts with patch versions', () => {
    expect(sortVersionsAsc(['7.8.3', '7.8.1', '7.8.2'])).toEqual(['7.8.1', '7.8.2', '7.8.3']);
  });

  it('does not mutate the original array', () => {
    const original = ['9.0', '7.8', '8.1'];
    const copy = [...original];
    sortVersionsAsc(original);

    expect(original).toEqual(copy);
  });

  it('returns empty array for empty input', () => {
    expect(sortVersionsAsc([])).toEqual([]);
  });

  it('returns single-element array unchanged', () => {
    expect(sortVersionsAsc(['7.8'])).toEqual(['7.8']);
  });

  it('handles many branches in the right order', () => {
    const input = ['9.1', '7.0', '8.0', '7.7', '9.0', '8.1', '7.8'];

    expect(sortVersionsAsc(input)).toEqual(['7.0', '7.7', '7.8', '8.0', '8.1', '9.0', '9.1']);
  });

  it('sorts versions with v prefix', () => {
    const result = sortVersionsAsc(['v9.0', 'v7.8', 'v8.1']);

    expect(result[0]).toContain('7');
    expect(result.at(-1)).toContain('9');
  });
});

describe('sortVersionsDesc', () => {
  it('sorts a simple list descending', () => {
    expect(sortVersionsDesc(['9.0', '7.8', '8.1'])).toEqual(['9.0', '8.1', '7.8']);
  });

  it('does not mutate the original array (desc)', () => {
    const original = ['9.0', '7.8', '8.1'];
    const copy = [...original];
    sortVersionsDesc(original);

    expect(original).toEqual(copy);
  });

  it('returns empty array for empty input (desc)', () => {
    expect(sortVersionsDesc([])).toEqual([]);
  });

  it('handles many branches in the right order (desc)', () => {
    const input = ['9.1', '7.0', '8.0', '7.7', '9.0', '8.1', '7.8'];

    expect(sortVersionsDesc(input)).toEqual(['9.1', '9.0', '8.1', '8.0', '7.8', '7.7', '7.0']);
  });
});
