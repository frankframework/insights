import { Component, inject, HostListener } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage, AsyncPipe, DatePipe } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { LocationService } from '../../services/location.service';
import { GraphStateService } from '../../services/graph-state.service';
import { VersionService } from '../../services/version.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [NgOptimizedImage, RouterLink, RouterLinkActive, AsyncPipe, DatePipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  public authService = inject(AuthService);
  public showUserMenu = false;
  public graphStateService = inject(GraphStateService);
  public buildInfo$ = inject(VersionService).getBuildInformation();

  private locationService = inject(LocationService);

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
}
