import { BadgeClassPipe } from './badge-class.pipe';

describe('BadgeClassPipe', () => {
  let pipe: BadgeClassPipe;

  beforeEach(() => {
    pipe = new BadgeClassPipe();
  });

  describe('null / undefined input', () => {
    it('returns badge-none for null', () => {
      expect(pipe.transform(null)).toBe('badge-none');
    });

    it('returns badge-none for undefined', () => {
      expect(pipe.transform(undefined)).toBe('badge-none');
    });
  });

  describe('string severity inputs', () => {
    it('returns badge-critical for CRITICAL', () => {
      expect(pipe.transform('CRITICAL')).toBe('badge-critical');
    });

    it('is case-insensitive for critical', () => {
      expect(pipe.transform('critical')).toBe('badge-critical');
      expect(pipe.transform('Critical')).toBe('badge-critical');
    });

    it('returns badge-high for HIGH', () => {
      expect(pipe.transform('HIGH')).toBe('badge-high');
    });

    it('is case-insensitive for high', () => {
      expect(pipe.transform('high')).toBe('badge-high');
    });

    it('returns badge-medium for MEDIUM', () => {
      expect(pipe.transform('MEDIUM')).toBe('badge-medium');
    });

    it('is case-insensitive for medium', () => {
      expect(pipe.transform('medium')).toBe('badge-medium');
    });

    it('returns badge-low for LOW', () => {
      expect(pipe.transform('LOW')).toBe('badge-low');
    });

    it('is case-insensitive for low', () => {
      expect(pipe.transform('low')).toBe('badge-low');
    });

    it('returns badge-none for unknown severity string', () => {
      expect(pipe.transform('UNKNOWN')).toBe('badge-none');
    });

    it('returns badge-none for empty string', () => {
      expect(pipe.transform('')).toBe('badge-none');
    });

    it('returns badge-none for arbitrary string', () => {
      expect(pipe.transform('MODERATE')).toBe('badge-none');
    });
  });

  describe('numeric impact score inputs', () => {
    it('returns badge-critical for score >= 7.5', () => {
      expect(pipe.transform(10.0)).toBe('badge-critical');
      expect(pipe.transform(9.8)).toBe('badge-critical');
      expect(pipe.transform(7.5)).toBe('badge-critical');
    });

    it('returns badge-high for score >= 5 and < 7.5', () => {
      expect(pipe.transform(7.4)).toBe('badge-high');
      expect(pipe.transform(5.0)).toBe('badge-high');
      expect(pipe.transform(6.0)).toBe('badge-high');
    });

    it('returns badge-medium for score >= 2.5 and < 5', () => {
      expect(pipe.transform(4.9)).toBe('badge-medium');
      expect(pipe.transform(2.5)).toBe('badge-medium');
      expect(pipe.transform(3.5)).toBe('badge-medium');
    });

    it('returns badge-low for score < 2.5', () => {
      expect(pipe.transform(2.4)).toBe('badge-low');
      expect(pipe.transform(0)).toBe('badge-low');
      expect(pipe.transform(1.0)).toBe('badge-low');
    });

    it('boundary: exactly 7.5 is badge-critical', () => {
      expect(pipe.transform(7.5)).toBe('badge-critical');
    });

    it('boundary: exactly 5.0 is badge-high', () => {
      expect(pipe.transform(5.0)).toBe('badge-high');
    });

    it('boundary: exactly 2.5 is badge-medium', () => {
      expect(pipe.transform(2.5)).toBe('badge-medium');
    });

    it('boundary: 0 is badge-low', () => {
      expect(pipe.transform(0)).toBe('badge-low');
    });
  });
});
