import { PriorityLabelPipe } from './priority-label.pipe';

describe('PriorityLabelPipe', () => {
  let pipe: PriorityLabelPipe;

  beforeEach(() => {
    pipe = new PriorityLabelPipe();
  });

  describe('null / undefined inputs (not assessed)', () => {
    it('returns Not assessed for null', () => {
      expect(pipe.transform(null)).toBe('Not assessed');
    });

    it('returns Not assessed for undefined', () => {
      expect(pipe.transform(undefined)).toBe('Not assessed');
    });
  });

  describe('priority tiers', () => {
    it('returns Critical for score >= 7.5', () => {
      expect(pipe.transform(10.0)).toBe('Critical');
      expect(pipe.transform(9.8)).toBe('Critical');
      expect(pipe.transform(7.5)).toBe('Critical');
    });

    it('returns High for score >= 5 and < 7.5', () => {
      expect(pipe.transform(7.4)).toBe('High');
      expect(pipe.transform(5.0)).toBe('High');
      expect(pipe.transform(6.0)).toBe('High');
    });

    it('returns Medium for score >= 2.5 and < 5', () => {
      expect(pipe.transform(4.9)).toBe('Medium');
      expect(pipe.transform(2.5)).toBe('Medium');
      expect(pipe.transform(3.5)).toBe('Medium');
    });

    it('returns Low for score < 2.5', () => {
      expect(pipe.transform(2.4)).toBe('Low');
      expect(pipe.transform(0)).toBe('Low');
      expect(pipe.transform(1.0)).toBe('Low');
    });
  });

  describe('boundary values', () => {
    it('boundary 7.5 is Critical', () => {
      expect(pipe.transform(7.5)).toBe('Critical');
    });

    it('boundary 5.0 is High', () => {
      expect(pipe.transform(5.0)).toBe('High');
    });

    it('boundary 2.5 is Medium', () => {
      expect(pipe.transform(2.5)).toBe('Medium');
    });

    it('score 0 is Low', () => {
      expect(pipe.transform(0)).toBe('Low');
    });
  });
});
