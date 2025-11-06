import { Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  imports: [NgOptimizedImage, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent implements OnInit {
  public authService = inject(AuthService);

  ngOnInit(): void {
    this.authService.checkAuthStatus().subscribe();
  }

  onLoginWithGitHub(): void {
    // Full page redirect to GitHub OAuth
    globalThis.location.href = '/oauth2/authorization/github';
  }

  onLogout(): void {
    this.authService.logout().subscribe();
  }

  onDismissError(): void {
    this.authService.clearError();
  }
}
