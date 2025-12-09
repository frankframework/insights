import { Component, inject, HostListener } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { LocationService } from '../../services/location.service';
import { GraphStateService } from '../../services/graph-state.service';

@Component({
  selector: 'app-header',
  imports: [NgOptimizedImage, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  public authService = inject(AuthService);
  public showUserMenu = false;
  private locationService = inject(LocationService);
  private graphStateService = inject(GraphStateService);
  private router = inject(Router);

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const userProfile = target.closest('.user-profile');

    if (!userProfile && this.showUserMenu) {
      this.showUserMenu = false;
    }
  }

  onLoginWithGitHub(): void {
    this.authService.setLoading(true);
    // Save current extended state temporarily for OAuth flow
    this.graphStateService.saveExtendedForOAuth(this.graphStateService.getShowExtendedSupport());
    this.locationService.navigateTo('/oauth2/authorization/github');
  }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  closeUserMenu(): void {
    this.showUserMenu = false;
  }

  onLogout(): void {
    this.closeUserMenu();
    this.authService.logout().subscribe();
  }

  onDismissError(): void {
    this.authService.clearError();
  }

  navigateToGraph(): void {
    const queryParams = this.graphStateService.getGraphQueryParams();
    this.router.navigate(['/graph'], { queryParams });
  }

  navigateToRoadmap(): void {
    this.router.navigate(['/roadmap']);
  }

  navigateToHome(): void {
    const queryParams = this.graphStateService.getGraphQueryParams();
    this.router.navigate(['/'], { queryParams });
  }
}
