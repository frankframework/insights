import { Component, inject, HostListener } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { LocationService } from '../../services/location.service';
import { GraphStateService } from '../../services/graph-state.service';
import { PillButtonComponent } from '../../components/pill-button/pill-button.component';
import { NavigateNewTabDirective } from '../../directives/navigate-new-tab.directive';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [NgOptimizedImage, RouterLink, RouterLinkActive, PillButtonComponent, NavigateNewTabDirective],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  public authService = inject(AuthService);
  public showUserMenu = false;
  public graphStateService: GraphStateService = inject(GraphStateService);

  private locationService = inject(LocationService);
  private router = inject(Router);

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const userProfile = target.closest('.user-profile');

    if (!userProfile && this.showUserMenu) {
      this.showUserMenu = false;
    }
  }

  public onLoginWithGitHub(): void {
    this.authService.setLoading(true);
    this.authService.setPendingAuth();
    this.rememberReturnUrl();
    this.graphStateService.saveExtendedForOAuth(this.graphStateService.getShowExtendedSupport());
    this.graphStateService.saveNightlyForOAuth(this.graphStateService.getShowNightlies());
    this.locationService.navigateTo('/oauth2/authorization/github');
  }

  public toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  public onLogout(): void {
    this.closeUserMenu();
    this.authService.logout().subscribe();
  }

  public onDismissError(): void {
    this.authService.clearError();
  }

  private closeUserMenu(): void {
    this.showUserMenu = false;
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
