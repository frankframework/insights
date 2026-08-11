import { TestBed } from '@angular/core/testing';
import { GraphStateService } from './graph-state.service';
import { parseVersionRanges, VersionRange } from '../pipes/release-range';

const rangesOf = (specification: string): VersionRange[] => parseVersionRanges(specification).ranges;

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
    it('should initialize with extended support level 0', () => {
      expect(service.extendedSupportLevel()).toBe(0);
      expect(service.showExtendedSupport()).toBe(false);
    });
  });

  describe('parseExtendedSupportLevel', () => {
    it('should return 0 when the param is absent', () => {
      expect(GraphStateService.parseExtendedSupportLevel()).toBe(0);
      expect(GraphStateService.parseExtendedSupportLevel(null)).toBe(0);
    });

    it('should return 1 for a bare extended param', () => {
      expect(GraphStateService.parseExtendedSupportLevel('')).toBe(1);
    });

    it('should return the requested level', () => {
      expect(GraphStateService.parseExtendedSupportLevel('1')).toBe(1);
      expect(GraphStateService.parseExtendedSupportLevel('2')).toBe(2);
      expect(GraphStateService.parseExtendedSupportLevel('3')).toBe(3);
    });

    it('should cap the level at 3', () => {
      expect(GraphStateService.parseExtendedSupportLevel('4')).toBe(3);
      expect(GraphStateService.parseExtendedSupportLevel('99')).toBe(3);
    });

    it('should treat 0 and negative levels as disabled', () => {
      expect(GraphStateService.parseExtendedSupportLevel('0')).toBe(0);
      expect(GraphStateService.parseExtendedSupportLevel('-2')).toBe(0);
    });

    it('should fall back to a single window for non numeric values', () => {
      expect(GraphStateService.parseExtendedSupportLevel('yes')).toBe(1);
    });
  });

  describe('setExtendedSupportLevel', () => {
    it('should update the extended support level', () => {
      service.setExtendedSupportLevel(2);

      expect(service.extendedSupportLevel()).toBe(2);
      expect(service.showExtendedSupport()).toBe(true);
    });

    it('should reset the extended support level to 0', () => {
      service.setExtendedSupportLevel(2);
      service.setExtendedSupportLevel(0);

      expect(service.extendedSupportLevel()).toBe(0);
      expect(service.showExtendedSupport()).toBe(false);
    });

    it('should clamp the level to the supported range', () => {
      service.setExtendedSupportLevel(10);

      expect(service.extendedSupportLevel()).toBe(3);

      service.setExtendedSupportLevel(-1);

      expect(service.extendedSupportLevel()).toBe(0);
    });
  });

  describe('graphQueryParams', () => {
    it('should return empty object when the extended support level is 0', () => {
      service.setExtendedSupportLevel(0);

      expect(service.graphQueryParams()).toEqual({});
    });

    it('should return the extended param with the current level', () => {
      service.setExtendedSupportLevel(1);

      expect(service.graphQueryParams()).toEqual({ extended: '1' });

      service.setExtendedSupportLevel(3);

      expect(service.graphQueryParams()).toEqual({ extended: '3' });
    });

    it('should return the range param when a range is set', () => {
      service.setReleaseRanges(rangesOf('[7.0,9.3.2]'));

      expect(service.graphQueryParams()).toEqual({ range: '[7.0,9.3.2]' });
    });
  });

  describe('setReleaseRanges', () => {
    it('should initialize without a range', () => {
      expect(service.releaseRanges()).toEqual([]);
    });

    it('should update the range', () => {
      const ranges = rangesOf('[8.0],[8.3]');
      service.setReleaseRanges(ranges);

      expect(service.releaseRanges()).toEqual(ranges);
    });
  });

  describe('range OAuth temporary storage', () => {
    it('should save the range to temporary localStorage', () => {
      service.saveRangeForOAuth(rangesOf('[7.1,7.3]'));

      expect(globalThis.localStorage.getItem('oauth_temp_range')).toBe('[7.1,7.3]');
    });

    it('should clear temporary localStorage when there is no range', () => {
      globalThis.localStorage.setItem('oauth_temp_range', '[7.1]');
      service.saveRangeForOAuth([]);

      expect(globalThis.localStorage.getItem('oauth_temp_range')).toBeNull();
    });

    it('should restore and clear the range', () => {
      globalThis.localStorage.setItem('oauth_temp_range', '[7.1,7.3]');

      expect(service.restoreAndClearOAuthRange()).toEqual(rangesOf('[7.1,7.3]'));
      expect(globalThis.localStorage.getItem('oauth_temp_range')).toBeNull();
    });

    it('should restore nothing when no range was stored', () => {
      expect(service.restoreAndClearOAuthRange()).toEqual([]);
    });
  });

  describe('OAuth temporary storage', () => {
    describe('saveExtendedSupportLevelForOAuth', () => {
      it('should save the extended support level to temporary localStorage', () => {
        service.saveExtendedSupportLevelForOAuth(2);

        expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBe('2');
      });

      it('should remove temporary storage when saving level 0', () => {
        globalThis.localStorage.setItem('oauth_temp_extended', '2');

        service.saveExtendedSupportLevelForOAuth(0);

        expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull();
      });

      it('should not affect the in-memory state', () => {
        service.setExtendedSupportLevel(0);
        service.saveExtendedSupportLevelForOAuth(2);

        expect(service.extendedSupportLevel()).toBe(0);
      });
    });

    describe('restoreAndClearOAuthExtendedSupportLevel', () => {
      it('should return the stored level', () => {
        globalThis.localStorage.setItem('oauth_temp_extended', '3');

        const result = service.restoreAndClearOAuthExtendedSupportLevel();

        expect(result).toBe(3);
      });

      it('should restore a legacy true value as a single extended window', () => {
        globalThis.localStorage.setItem('oauth_temp_extended', 'true');

        const result = service.restoreAndClearOAuthExtendedSupportLevel();

        expect(result).toBe(1);
      });

      it('should return 0 when temporary storage is empty', () => {
        const result = service.restoreAndClearOAuthExtendedSupportLevel();

        expect(result).toBe(0);
      });

      it('should clear temporary storage after restoring', () => {
        globalThis.localStorage.setItem('oauth_temp_extended', '2');

        service.restoreAndClearOAuthExtendedSupportLevel();

        expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull();
      });

      it('should not throw when localStorage is empty', () => {
        expect(() => service.restoreAndClearOAuthExtendedSupportLevel()).not.toThrow();
      });
    });
  });

  describe('OAuth flow integration', () => {
    it('should handle complete OAuth flow correctly', () => {
      // Step 1: User is on /graph?extended=2
      service.setExtendedSupportLevel(2);

      expect(service.extendedSupportLevel()).toBe(2);

      // Step 2: User clicks login - save state temporarily
      service.saveExtendedSupportLevelForOAuth(2);

      expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBe('2');

      // Step 3: Simulate page reload by resetting the service state manually
      service.setExtendedSupportLevel(0);

      expect(service.extendedSupportLevel()).toBe(0);

      // Step 4: Restore from temp storage after OAuth redirect
      const restoredLevel = service.restoreAndClearOAuthExtendedSupportLevel();

      expect(restoredLevel).toBe(2);
      expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull(); // cleaned up

      // Step 5: App sets state based on restored value
      service.setExtendedSupportLevel(restoredLevel);

      expect(service.extendedSupportLevel()).toBe(2);
    });

    it('should handle OAuth flow when extended was disabled', () => {
      // User is on /graph (no extended param)
      service.setExtendedSupportLevel(0);

      // Save state temporarily before OAuth
      service.saveExtendedSupportLevelForOAuth(0);

      expect(globalThis.localStorage.getItem('oauth_temp_extended')).toBeNull();

      // After OAuth redirect
      const restoredLevel = service.restoreAndClearOAuthExtendedSupportLevel();

      expect(restoredLevel).toBe(0);
    });
  });

  describe('URL as source of truth', () => {
    it('should allow multiple state changes without persistent storage', () => {
      service.setExtendedSupportLevel(1);

      expect(service.extendedSupportLevel()).toBe(1);

      service.setExtendedSupportLevel(0);

      expect(service.extendedSupportLevel()).toBe(0);

      service.setExtendedSupportLevel(3);

      expect(service.extendedSupportLevel()).toBe(3);

      // Verify no persistent storage was created (only temp OAuth storage would be in localStorage)
      expect(globalThis.localStorage.getItem('extended_support')).toBeNull();
    });

    it('should not create persistent storage for normal state changes', () => {
      service.setExtendedSupportLevel(1);

      // Verify that no persistent localStorage key is created for normal operations
      expect(globalThis.localStorage.getItem('extended_support')).toBeNull();

      service.setExtendedSupportLevel(0);

      // Still no persistent storage
      expect(globalThis.localStorage.getItem('extended_support')).toBeNull();
    });
  });
});
