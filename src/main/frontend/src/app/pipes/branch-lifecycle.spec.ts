import { isBranchMaintained } from './branch-lifecycle';

const monthsAgo = (n: number): Date => {
  const d = new Date();
  d.setMonth(d.getMonth() - n);
  return d;
};

describe('isBranchMaintained', () => {
  describe('special branch names always maintained', () => {
    it('master is always maintained', () => {
      expect(isBranchMaintained('master', monthsAgo(24))).toBeTrue();
    });

    it('release/master is always maintained', () => {
      expect(isBranchMaintained('release/master', monthsAgo(24))).toBeTrue();
    });

    it('nightly branch is always maintained', () => {
      expect(isBranchMaintained('nightly', monthsAgo(24))).toBeTrue();
    });

    it('branch ending with nightly is always maintained', () => {
      expect(isBranchMaintained('release/9.1-nightly', monthsAgo(24))).toBeTrue();
    });
  });

  describe('minor branch (minor > 0): 6 months support', () => {
    it('minor branch within 6 months is maintained', () => {
      expect(isBranchMaintained('release/7.8', monthsAgo(3))).toBeTrue();
    });

    it('minor branch exactly at 6 months boundary is maintained', () => {
      expect(isBranchMaintained('7.8', monthsAgo(6))).toBeTrue();
    });

    it('minor branch just past 6 months is unmaintained', () => {
      expect(isBranchMaintained('7.8', monthsAgo(7))).toBeFalse();
    });

    it('minor branch 12 months ago is unmaintained', () => {
      expect(isBranchMaintained('release/7.6', monthsAgo(12))).toBeFalse();
    });

    it('minor branch 1 day old is maintained', () => {
      const yesterday = new Date();
      yesterday.setDate(yesterday.getDate() - 1);

      expect(isBranchMaintained('release/8.3', yesterday)).toBeTrue();
    });
  });

  describe('major branch (minor = 0): 12 months support', () => {
    it('major branch within 12 months is maintained', () => {
      expect(isBranchMaintained('release/8.0', monthsAgo(6))).toBeTrue();
    });

    it('major branch exactly at 12 months boundary is maintained', () => {
      expect(isBranchMaintained('8.0', monthsAgo(12))).toBeTrue();
    });

    it('major branch just past 12 months is unmaintained', () => {
      expect(isBranchMaintained('8.0', monthsAgo(13))).toBeFalse();
    });

    it('major branch 24 months ago is unmaintained', () => {
      expect(isBranchMaintained('release/7.0', monthsAgo(24))).toBeFalse();
    });
  });

  describe('prefix stripping', () => {
    it('strips release/ prefix correctly', () => {
      const result7_8_with = isBranchMaintained('release/7.8', monthsAgo(3));
      const result7_8_without = isBranchMaintained('7.8', monthsAgo(3));

      expect(result7_8_with).toBe(result7_8_without);
    });

    it('handles version prefix v', () => {
      expect(isBranchMaintained('v8.1', monthsAgo(3))).toBeTrue();
    });
  });

  describe('unknown / unparseable branch names', () => {
    it('returns true for unknown branch pattern', () => {
      expect(isBranchMaintained('unknown-branch', monthsAgo(24))).toBeTrue();
    });

    it('returns true for empty pattern', () => {
      expect(isBranchMaintained('', monthsAgo(24))).toBeTrue();
    });
  });

  describe('date input types', () => {
    it('accepts a Date object', () => {
      const date = monthsAgo(3);

      expect(isBranchMaintained('release/8.1', date)).toBeTrue();
    });

    it('accepts an ISO date string', () => {
      const isoString = monthsAgo(3).toISOString();

      expect(isBranchMaintained('release/8.1', isoString)).toBeTrue();
    });
  });
});
