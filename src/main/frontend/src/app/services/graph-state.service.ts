import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class GraphStateService {
  private static readonly OAUTH_TEMP_KEY = 'oauth_temp_extended';
  private showExtendedSupport = signal<boolean>(false);

  public getShowExtendedSupport(): boolean {
    return this.showExtendedSupport();
  }

  public setShowExtendedSupport(value: boolean): void {
    this.showExtendedSupport.set(value);
  }

  public getGraphQueryParams(): { [key: string]: string } {
    return this.showExtendedSupport() ? { extended: '' } : {};
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
