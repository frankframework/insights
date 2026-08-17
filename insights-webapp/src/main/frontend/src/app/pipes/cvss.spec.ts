import { CvssVector, calculateCvssScore, isVectorComplete, parseVectorString, severityForScore, vectorToString } from './cvss';

describe('cvss', () => {
  describe('calculateCvssScore', () => {
    it('returns null for an incomplete vector', () => {
      expect(calculateCvssScore({ AV: 'N', AC: 'L' })).toBeNull();
    });

    it('matches the published score for CVE-2021-44228 (Log4Shell)', () => {
      const vector: CvssVector = { AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'C', C: 'H', I: 'H', A: 'H' };

      expect(calculateCvssScore(vector)).toEqual({ score: 10, severity: 'Critical' });
    });

    it('matches the hand-computed score for a network, low-complexity, no-interaction attack', () => {
      const vector: CvssVector = { AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U', C: 'H', I: 'H', A: 'H' };

      expect(calculateCvssScore(vector)).toEqual({ score: 9.8, severity: 'Critical' });
    });

    it('returns a 0 score and None severity when there is no impact', () => {
      const vector: CvssVector = { AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U', C: 'N', I: 'N', A: 'N' };

      expect(calculateCvssScore(vector)).toEqual({ score: 0, severity: 'None' });
    });
  });

  describe('severityForScore', () => {
    it('maps scores to the official CVSS v3.1 qualitative severity ratings', () => {
      expect(severityForScore(0)).toBe('None');
      expect(severityForScore(3.9)).toBe('Low');
      expect(severityForScore(4)).toBe('Medium');
      expect(severityForScore(6.9)).toBe('Medium');
      expect(severityForScore(7)).toBe('High');
      expect(severityForScore(8.9)).toBe('High');
      expect(severityForScore(9)).toBe('Critical');
      expect(severityForScore(10)).toBe('Critical');
    });
  });

  describe('isVectorComplete', () => {
    it('returns false when any metric is missing', () => {
      expect(isVectorComplete({ AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U', C: 'H', I: 'H' })).toBe(false);
    });

    it('returns true when all eight metrics are present', () => {
      expect(
        isVectorComplete({ AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U', C: 'H', I: 'H', A: 'H' })
      ).toBe(true);
    });
  });

  describe('vectorToString / parseVectorString', () => {
    const vector: CvssVector = { AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U', C: 'H', I: 'H', A: 'H' };
    const vectorString = 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H';

    it('serializes a complete vector in the canonical metric order', () => {
      expect(vectorToString(vector)).toBe(vectorString);
    });

    it('parses a canonical vector string back into a vector', () => {
      expect(parseVectorString(vectorString)).toEqual(vector);
    });

    it('parses a vector string without the CVSS:3.1 prefix', () => {
      expect(parseVectorString('AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H')).toEqual(vector);
    });

    it('is case-insensitive for the prefix and metric values', () => {
      expect(parseVectorString(vectorString.toLowerCase())).toEqual(vector);
    });

    it('returns null for an incomplete vector string', () => {
      expect(parseVectorString('CVSS:3.1/AV:N/AC:L')).toBeNull();
    });

    it('returns null for empty input', () => {
      expect(parseVectorString('   ')).toBeNull();
    });

    it('ignores unknown or malformed segments', () => {
      expect(parseVectorString('CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/XX:Z/garbage')).toEqual(vector);
    });
  });
});
