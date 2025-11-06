import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  imports: [NgOptimizedImage, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  public authService = inject(AuthService);
  public showUserMenu = false;

  onLoginWithGitHub(): void {
    this.authService.setLoading(true);
    globalThis.location.href = '/oauth2/authorization/github';
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
