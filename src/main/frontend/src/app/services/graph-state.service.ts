import { Injectable, Signal, WritableSignal, computed, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class GraphStateService {
  public static readonly MAX_EXTENDED_SUPPORT_LEVEL: number = 3;

  private static readonly OAUTH_TEMP_EXTENDED_KEY: string = 'oauth_temp_extended';
  private static readonly OAUTH_TEMP_NIGHTLY_KEY: string = 'oauth_temp_nightly';

  public readonly extendedSupportLevel: Signal<number>;
  public readonly showNightlies: Signal<boolean>;
  public readonly showExtendedSupport: Signal<boolean>;
  public readonly graphQueryParams: Signal<Record<string, string>>;

  private readonly extendedSupportLevelState: WritableSignal<number> = signal<number>(0);
  private readonly showNightliesState: WritableSignal<boolean> = signal<boolean>(false);

  constructor() {
    this.extendedSupportLevel = this.extendedSupportLevelState.asReadonly();
    this.showNightlies = this.showNightliesState.asReadonly();
    this.showExtendedSupport = computed(() => this.extendedSupportLevelState() > 0);
    this.graphQueryParams = computed(() => {
      const parameters: Record<string, string> = {};
      const level = this.extendedSupportLevelState();
      if (level > 0) parameters['extended'] = String(level);
      if (this.showNightliesState()) parameters['nightly'] = '';
      return parameters;
    });
  }

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

  public setExtendedSupportLevel(level: number): void {
    this.extendedSupportLevelState.set(GraphStateService.clampExtendedSupportLevel(level));
  }

  public setShowNightlies(value: boolean): void {
    this.showNightliesState.set(value);
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
