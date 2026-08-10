import { Injectable, signal, WritableSignal } from '@angular/core';
import { parseVersionRanges, serializeVersionRanges, VersionRange } from '../pipes/release-range';

@Injectable({
  providedIn: 'root',
})
export class GraphStateService {
  public static readonly MAX_EXTENDED_SUPPORT_LEVEL: number = 3;

  private static readonly OAUTH_TEMP_EXTENDED_KEY: string = 'oauth_temp_extended';
  private static readonly OAUTH_TEMP_NIGHTLY_KEY: string = 'oauth_temp_nightly';
  private static readonly OAUTH_TEMP_RANGE_KEY: string = 'oauth_temp_range';
  private extendedSupportLevel: WritableSignal<number> = signal<number>(0);
  private showNightlies: WritableSignal<boolean> = signal<boolean>(false);
  private releaseRanges: WritableSignal<VersionRange[]> = signal<VersionRange[]>([]);

  public static parseExtendedSupportLevel(value?: string | null): number {
    if (value === null || value === undefined) return 0;
    if (value === '') return 1;

    const parsedLevel = Number.parseInt(value, 10);
    if (Number.isNaN(parsedLevel)) return 1;

    return GraphStateService.clampExtendedSupportLevel(parsedLevel);
  }

  private static clampExtendedSupportLevel(level: number): number {
    return Math.min(Math.max(Math.trunc(level), 0), GraphStateService.MAX_EXTENDED_SUPPORT_LEVEL);
  }

  public getExtendedSupportLevel(): number {
    return this.extendedSupportLevel();
  }

  public setExtendedSupportLevel(level: number): void {
    this.extendedSupportLevel.set(GraphStateService.clampExtendedSupportLevel(level));
  }

  public getShowExtendedSupport(): boolean {
    return this.extendedSupportLevel() > 0;
  }

  public getShowNightlies(): boolean {
    return this.showNightlies();
  }

  public setShowNightlies(value: boolean): void {
    this.showNightlies.set(value);
  }

  public getReleaseRanges(): VersionRange[] {
    return this.releaseRanges();
  }

  public setReleaseRanges(ranges: VersionRange[]): void {
    this.releaseRanges.set(ranges);
  }

  public getGraphQueryParams(): Record<string, string> {
    const parameters: Record<string, string> = {};
    if (this.extendedSupportLevel() > 0) parameters['extended'] = String(this.extendedSupportLevel());
    if (this.showNightlies()) parameters['nightly'] = '';

    const range = serializeVersionRanges(this.releaseRanges());
    if (range) parameters['range'] = range;

    return parameters;
  }

  public saveRangeForOAuth(ranges: VersionRange[]): void {
    const serialized = serializeVersionRanges(ranges);
    if (serialized) {
      localStorage.setItem(GraphStateService.OAUTH_TEMP_RANGE_KEY, serialized);
    } else {
      localStorage.removeItem(GraphStateService.OAUTH_TEMP_RANGE_KEY);
    }
  }

  public restoreAndClearOAuthRange(): VersionRange[] {
    const stored = localStorage.getItem(GraphStateService.OAUTH_TEMP_RANGE_KEY);
    localStorage.removeItem(GraphStateService.OAUTH_TEMP_RANGE_KEY);
    return parseVersionRanges(stored).ranges;
  }

  public saveNightlyForOAuth(value: boolean): void {
    if (value) {
      localStorage.setItem(GraphStateService.OAUTH_TEMP_NIGHTLY_KEY, 'true');
    } else {
      localStorage.removeItem(GraphStateService.OAUTH_TEMP_NIGHTLY_KEY);
    }
  }

  public restoreAndClearOAuthNightly(): boolean {
    const stored = localStorage.getItem(GraphStateService.OAUTH_TEMP_NIGHTLY_KEY);
    localStorage.removeItem(GraphStateService.OAUTH_TEMP_NIGHTLY_KEY);
    return stored === 'true';
  }

  /**
   * Save the extended support level temporarily for the OAuth flow
   * This is used before redirecting to OAuth to preserve state across page reload
   */
  public saveExtendedSupportLevelForOAuth(level: number): void {
    const clampedLevel = GraphStateService.clampExtendedSupportLevel(level);
    if (clampedLevel > 0) {
      localStorage.setItem(GraphStateService.OAUTH_TEMP_EXTENDED_KEY, String(clampedLevel));
    } else {
      localStorage.removeItem(GraphStateService.OAUTH_TEMP_EXTENDED_KEY);
    }
  }

  /**
   * Restore and clear the temporary OAuth extended support level
   * Returns the saved level and immediately removes the temp storage.
   */
  public restoreAndClearOAuthExtendedSupportLevel(): number {
    const stored = localStorage.getItem(GraphStateService.OAUTH_TEMP_EXTENDED_KEY);
    localStorage.removeItem(GraphStateService.OAUTH_TEMP_EXTENDED_KEY);
    return GraphStateService.parseExtendedSupportLevel(stored);
  }
}
