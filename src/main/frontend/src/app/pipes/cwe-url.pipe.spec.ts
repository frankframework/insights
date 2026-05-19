import { CweUrlPipe } from './cwe-url.pipe';

describe('CweUrlPipe', () => {
  let pipe: CweUrlPipe;

  beforeEach(() => {
    pipe = new CweUrlPipe();
  });

  describe('happy flow', () => {
    it('generates correct MITRE URL for CWE-79', () => {
      expect(pipe.transform('CWE-79')).toBe('https://cwe.mitre.org/data/definitions/79.html');
    });

    it('generates correct MITRE URL for CWE-89', () => {
      expect(pipe.transform('CWE-89')).toBe('https://cwe.mitre.org/data/definitions/89.html');
    });

    it('generates correct MITRE URL for a three-digit CWE', () => {
      expect(pipe.transform('CWE-200')).toBe('https://cwe.mitre.org/data/definitions/200.html');
    });

    it('generates correct MITRE URL for a four-digit CWE', () => {
      expect(pipe.transform('CWE-1234')).toBe('https://cwe.mitre.org/data/definitions/1234.html');
    });

    it('extracts number regardless of prefix format', () => {
      expect(pipe.transform('cwe-79')).toBe('https://cwe.mitre.org/data/definitions/79.html');
    });

    it('extracts just the number when input is a plain number string', () => {
      expect(pipe.transform('79')).toBe('https://cwe.mitre.org/data/definitions/79.html');
    });
  });

  describe('edge cases and unhappy flow', () => {
    it('returns # for input with no digits', () => {
      expect(pipe.transform('CWE-')).toBe('#');
    });

    it('returns # for input without a number', () => {
      expect(pipe.transform('NO-NUMBER')).toBe('#');
    });

    it('returns # for empty string', () => {
      expect(pipe.transform('')).toBe('#');
    });

    it('uses first number found when multiple numbers present', () => {
      const result = pipe.transform('CWE-79-and-89');

      expect(result).toBe('https://cwe.mitre.org/data/definitions/79.html');
    });
  });
});
