import { Component, inject, HostListener } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage, AsyncPipe, DatePipe } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { LocationService } from '../../services/location.service';
import { GraphStateService } from '../../services/graph-state.service';
import { BuildInfo, VersionService } from '../../services/version.service';
import { Observable } from 'rxjs';

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
  public graphStateService: GraphStateService = inject(GraphStateService);
  public buildInfo$: Observable<BuildInfo | null> = inject(VersionService).getBuildInformation();

  private locationService = inject(LocationService);

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
    this.graphStateService.saveExtendedForOAuth(this.graphStateService.getShowExtendedSupport());
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

  public formatVersion(version: string): string {
    return version ? version.replace(/^0\.0\./, '') : '';
  }

  private closeUserMenu(): void {
    this.showUserMenu = false;
  }
}
