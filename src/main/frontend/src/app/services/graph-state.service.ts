import { Injectable, signal, WritableSignal } from '@angular/core';
import { parseVersionRanges, serializeVersionRanges, VersionRange } from '../pipes/release-range';

@Injectable({
  providedIn: 'root',
})
export class GraphStateService {
  private static readonly OAUTH_TEMP_KEY: string = 'oauth_temp_extended';
  private static readonly OAUTH_TEMP_NIGHTLY_KEY: string = 'oauth_temp_nightly';
  private static readonly OAUTH_TEMP_RANGE_KEY: string = 'oauth_temp_range';
  private showExtendedSupport: WritableSignal<boolean> = signal<boolean>(false);
  private showNightlies: WritableSignal<boolean> = signal<boolean>(false);
  private releaseRanges: WritableSignal<VersionRange[]> = signal<VersionRange[]>([]);

  public getShowExtendedSupport(): boolean {
    return this.showExtendedSupport();
  }

  public setShowExtendedSupport(value: boolean): void {
    this.showExtendedSupport.set(value);
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
    if (this.showExtendedSupport()) parameters['extended'] = '';
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
   * Save extended state temporarily for OAuth flow
   * This is used before redirecting to OAuth to preserve state across page reload
   */
  public saveExtendedForOAuth(value: boolean): void {
    if (value) {
      localStorage.setItem(GraphStateService.OAUTH_TEMP_KEY, 'true');
    } else {
      localStorage.removeItem(GraphStateService.OAUTH_TEMP_KEY);
    }
  }

  /**
   * Restore and clear temporary OAuth state
   * Returns the saved value and immediately removes the temp storage
   */
  public restoreAndClearOAuthExtended(): boolean {
    const stored = localStorage.getItem(GraphStateService.OAUTH_TEMP_KEY);
    localStorage.removeItem(GraphStateService.OAUTH_TEMP_KEY);
    return stored === 'true';
  }
}
