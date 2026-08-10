import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { LocationService } from '../../services/location.service';
import { GraphStateService } from '../../services/graph-state.service';
import { PillButtonComponent } from '../../components/pill-button/pill-button.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [NgOptimizedImage, RouterLink, RouterLinkActive, PillButtonComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
  host: { '(document:click)': 'onDocumentClick($event)' },
})
export class HeaderComponent {
  public readonly authService = inject(AuthService);
  public readonly graphStateService = inject(GraphStateService);
  public readonly showUserMenu = signal(false);

  private readonly locationService = inject(LocationService);
  private readonly router = inject(Router);

  public onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const userProfile = target.closest('.user-profile');

    if (!userProfile) {
      this.closeUserMenu();
    }
  }

  public onLoginWithGitHub(): void {
    this.authService.setLoading(true);
    this.authService.setPendingAuth();
    this.rememberReturnUrl();
    this.graphStateService.saveExtendedSupportLevelForOAuth(this.graphStateService.extendedSupportLevel());
    this.graphStateService.saveNightlyForOAuth(this.graphStateService.showNightlies());
    this.graphStateService.saveRangeForOAuth(this.graphStateService.releaseRanges());
    this.locationService.navigateTo('/oauth2/authorization/github');
  }

  public toggleUserMenu(): void {
    this.showUserMenu.update((open) => !open);
  }

  public onLogout(): void {
    this.closeUserMenu();
    this.authService.logout().subscribe();
  }

  public onDismissError(): void {
    this.authService.clearError();
  }

  private closeUserMenu(): void {
    this.showUserMenu.set(false);
  }

  /**
   * Remember the current page so the user is returned here after login. The graph/root
   * page is the OAuth default landing (and restores its own view state), so skip it.
   */
  private rememberReturnUrl(): void {
    const currentUrl = this.router.url;
    const path = currentUrl.split('?')[0];
    const isDefaultLanding = path === '' || path === '/' || path.startsWith('/graph');

    if (!isDefaultLanding) {
      this.authService.saveReturnUrl(currentUrl);
    }
  }
}
