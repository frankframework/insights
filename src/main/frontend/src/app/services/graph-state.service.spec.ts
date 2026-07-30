import { TestBed } from '@angular/core/testing';
import { GraphStateService } from './graph-state.service';
import { parseIncludeRanges } from '../pipes/release-include';

describe('GraphStateService', () => {
  let service: GraphStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GraphStateService);
    // Clear localStorage before each test
    globalThis.localStorage.clear();
  });

  afterEach(() => {
    // Clean up after each test
    globalThis.localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Initial State', () => {
    it('should initialize with showExtendedSupport as false', () => {
      expect(service.getShowExtendedSupport()).toBe(false);
    });
  });

  describe('setShowExtendedSupport', () => {
    it('should update the extended support state to true', () => {
      service.setShowExtendedSupport(true);

      expect(service.getShowExtendedSupport()).toBe(true);
    });

    it('should update the extended support state to false', () => {
      service.setShowExtendedSupport(true);
      service.setShowExtendedSupport(false);

      expect(service.getShowExtendedSupport()).toBe(false);
    });
  });

  describe('getGraphQueryParams', () => {
    it('should return empty object when showExtendedSupport is false', () => {
      service.setShowExtendedSupport(false);

      expect(service.getGraphQueryParams()).toEqual({});
    });

    it('should return extended param when showExtendedSupport is true', () => {
      service.setShowExtendedSupport(true);

      expect(service.getGraphQueryParams()).toEqual({ extended: '' });
    });

    it('should return include param when releases are included', () => {
      service.setIncludedReleases(parseIncludeRanges('7.0-9.3.2'));

      expect(service.getGraphQueryParams()).toEqual({ include: '7.0-9.3.2' });
    });
  });

  describe('setIncludedReleases', () => {
    it('should initialize without included releases', () => {
      expect(service.getIncludedReleases()).toEqual([]);
    });

    it('should update the included releases', () => {
      const ranges = parseIncludeRanges('8.0,8.1,8.3');
      service.setIncludedReleases(ranges);

      expect(service.getIncludedReleases()).toEqual(ranges);
    });
  });

  describe('include OAuth temporary storage', () => {
    it('should save included releases to temporary localStorage', () => {
      service.saveIncludeForOAuth(parseIncludeRanges('7.1-7.3'));

      expect(globalThis.localStorage.getItem('oauth_temp_include')).toBe('7.1-7.3');
    });

    it('should clear temporary localStorage when nothing is included', () => {
      globalThis.localStorage.setItem('oauth_temp_include', '7.1');
      service.saveIncludeForOAuth([]);

      expect(globalThis.localStorage.getItem('oauth_temp_include')).toBeNull();
    });

    it('should restore and clear the included releases', () => {
      globalThis.localStorage.setItem('oauth_temp_include', '7.1-7.3');

      expect(service.restoreAndClearOAuthInclude()).toEqual(parseIncludeRanges('7.1-7.3'));
      expect(globalThis.localStorage.getItem('oauth_temp_include')).toBeNull();
    });

    it('should restore nothing when no include was stored', () => {
      expect(service.restoreAndClearOAuthInclude()).toEqual([]);
    });
  });

  describe('OAuth temporary storage', () => {
    describe('saveExtendedForOAuth', () => {
      it('should save extended state to temporary localStorage', () => {
        service.saveExtendedForOAuth(true);

        expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBe('true');
      });

      it('should remove temporary storage when saving false', () => {
        globalThis.localStorage.setItem('oauth_temp_extended', 'true');

        service.saveExtendedForOAuth(false);

        expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull();
      });

      it('should not affect the in-memory state', () => {
        service.setShowExtendedSupport(false);
        service.saveExtendedForOAuth(true);

        expect(service.getShowExtendedSupport()).toBe(false);
      });
    });

    describe('restoreAndClearOAuthExtended', () => {
      it('should return true when temporary storage has true', () => {
        globalThis.localStorage.setItem('oauth_temp_extended', 'true');

        const result = service.restoreAndClearOAuthExtended();

        expect(result).toBe(true);
      });

      it('should return false when temporary storage is empty', () => {
        const result = service.restoreAndClearOAuthExtended();

        expect(result).toBe(false);
      });

      it('should clear temporary storage after restoring', () => {
        globalThis.localStorage.setItem('oauth_temp_extended', 'true');

        service.restoreAndClearOAuthExtended();

        expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull();
      });

      it('should not throw when localStorage is empty', () => {
        expect(() => service.restoreAndClearOAuthExtended()).not.toThrow();
      });
    });
  });

  describe('OAuth flow integration', () => {
    it('should handle complete OAuth flow correctly', () => {
      // Step 1: User is on /graph?extended
      service.setShowExtendedSupport(true);

      expect(service.getShowExtendedSupport()).toBe(true);

      // Step 2: User clicks login - save state temporarily
      service.saveExtendedForOAuth(true);

      expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBe('true');

      // Step 3: Simulate page reload by resetting the service state manually
      service.setShowExtendedSupport(false);

      expect(service.getShowExtendedSupport()).toBe(false);

      // Step 4: Restore from temp storage after OAuth redirect
      const wasExtended = service.restoreAndClearOAuthExtended();

      expect(wasExtended).toBe(true);
      expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull(); // cleaned up

      // Step 5: App sets state based on restored value
      service.setShowExtendedSupport(wasExtended);

      expect(service.getShowExtendedSupport()).toBe(true);
    });

    it('should handle OAuth flow when extended was false', () => {
      // User is on /graph (no extended param)
      service.setShowExtendedSupport(false);

      // Save state temporarily before OAuth
      service.saveExtendedForOAuth(false);

      expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull();

      // After OAuth redirect
      const wasExtended = service.restoreAndClearOAuthExtended();

      expect(wasExtended).toBe(false);
    });
  });

  describe('URL as source of truth', () => {
    it('should allow multiple state changes without persistent storage', () => {
      service.setShowExtendedSupport(true);

      expect(service.getShowExtendedSupport()).toBe(true);

      service.setShowExtendedSupport(false);

      expect(service.getShowExtendedSupport()).toBe(false);

      service.setShowExtendedSupport(true);

      expect(service.getShowExtendedSupport()).toBe(true);

      // Verify no persistent storage was created (only temp OAuth storage would be in localStorage)
      expect(globalThis.localStorage.getItem('extended_support')).toBeNull();
    });

    it('should not create persistent storage for normal state changes', () => {
      service.setShowExtendedSupport(true);

      // Verify that no persistent localStorage key is created for normal operations
      expect(globalThis.localStorage.getItem('extended_support')).toBeNull();

      service.setShowExtendedSupport(false);

      // Still no persistent storage
      expect(globalThis.localStorage.getItem('extended_support')).toBeNull();
    });
  });
});
